package ru.turbovadim.v2.processor

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerRespawnEvent
import ru.turbovadim.v2.ability.AbilityEffect
import ru.turbovadim.v2.di.OriginsContainer

/**
 * Resolves natural spawn locations supplied by active abilities.
 *
 * Beds and respawn anchors retain vanilla precedence. Multiple providers are
 * resolved by explicit priority and then by ability key for deterministic ties.
 */
class DefaultSpawnAbilityProcessor(private val container: OriginsContainer) : Listener {

    private data class Candidate(
        val abilityKey: Key,
        val effect: AbilityEffect.DefaultSpawn
    )

    fun registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, container.plugin)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        if (event.isBedSpawn || event.isAnchorSpawn) return
        event.respawnLocation = resolve(event.player, event.respawnLocation)
    }

    fun resolve(player: Player, fallback: Location): Location {
        val abilityKeys = container.playerStateManager.getState(player).getAbilityKeys()
        val candidates = abilityKeys
            .asSequence()
            .filter { isAbilityActive(player, it) }
            .flatMap { abilityKey ->
                container.abilityRegistry.get(abilityKey)
                    ?.effects
                    ?.filterIsInstance<AbilityEffect.DefaultSpawn>()
                    .orEmpty()
                    .asSequence()
                    .map { Candidate(abilityKey, it) }
            }
            .sortedWith(
                compareByDescending<Candidate> { it.effect.priority }
                    .thenBy { it.abilityKey.asString() }
            )

        for (candidate in candidates) {
            val ability = container.abilityRegistry.get(candidate.abilityKey) ?: continue
            val accessor = container.configLoader.getAccessor(candidate.abilityKey, ability.defaultOptions)
            candidate.effect.resolver.resolve(player, accessor)?.let { return it }
        }

        return fallback
    }

    private fun isAbilityActive(player: Player, abilityKey: Key): Boolean {
        val ability = container.abilityRegistry.get(abilityKey) ?: return false
        val dependencyKey = ability.dependencyKey ?: return true
        val dependency = container.abilityRegistry.getDependencyAbility(dependencyKey) ?: return true
        return dependency.isEnabled(player) != ability.dependencyInverse
    }
}
