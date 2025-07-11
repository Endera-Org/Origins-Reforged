package ru.turbovadim.abilities.types

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.kyori.adventure.key.Key
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.AddonLoader
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsAddon
import ru.turbovadim.OriginsReforged
import ru.turbovadim.abilities.AbilityRegister.abilityMap
import ru.turbovadim.util.WorldGuardHook

interface Ability {

    val key: Key

    suspend fun runForAbilityAsync(entity: Entity, runner: AsyncAbilityRunner) {
        runForAbilityAsync(entity, runner, null)
    }

    suspend fun runForAbilityAsync(entity: Entity, has: AsyncAbilityRunner?, other: AsyncAbilityRunner?) {
        if (entity is Player) {
            if (hasAbilityAsync(entity)) {
                has?.run(entity)
            } else {
                other?.run(entity)
            }
        }
    }

    fun runForAbility(entity: Entity, runner: AbilityRunner) {
        runForAbility(entity, runner, null)
    }

    fun runForAbility(entity: Entity, has: AbilityRunner?, other: AbilityRunner?) {
        if (entity is Player) {
            if (hasAbility(entity)) {
                has?.run(entity)
            } else {
                other?.run(entity)
            }
        }
    }

    suspend fun hasAbilityAsync(player: Player): Boolean = withContext(ioDispatcher) {
        computeHasAbility(player)
    }

    fun hasAbility(player: Player): Boolean {
        return computeHasAbilitySync(player)
    }

    private suspend fun computeHasAbility(player: Player): Boolean = withContext(ioDispatcher) {
        computeHasAbilitySync(player)
    }

    private fun computeHasAbilitySync(player: Player): Boolean {
        AddonLoader.abilityOverrideChecks
            .asSequence()
            .mapNotNull { it?.get(player, key) }
            .firstOrNull()
            ?.let { state ->
                return@computeHasAbilitySync when (state) {
                    OriginsAddon.State.DENY -> false
                    OriginsAddon.State.ALLOW -> true
                    else -> false
                }
            }

        if (OriginsReforged.Companion.isWorldGuardHookInitialized) {
            if (WorldGuardHook.isAbilityDisabled(player.location, this@Ability)) return false

            val section = OriginsReforged.mainConfig.preventAbilitiesIn
            if (section.isNotEmpty()) {
                val loc = BukkitAdapter.adapt(player.location)
                val container = WorldGuard.getInstance().platform.regionContainer
                val query = container.createQuery()
                val regions = query.getApplicableRegions(loc)
                val keyStr = key.toString()

                for (region in regions) {
                    section[region.id]?.takeIf { it.contains("all") || it.contains(keyStr) }?.let {
                        return false
                    }
                    section[region.id.lowercase()]?.takeIf { it.contains("all") || it.contains(keyStr) }?.let {
                        return false
                    }
                }
            }
        }

        val origins = OriginSwapper.getOriginsSync(player)
        var hasAbility = origins.any { it.hasAbility(key) }

        (abilityMap[key] as? DependantAbility)?.let { dependantAbility ->
            val dependencyEnabled = dependantAbility.dependency.isEnabled(player)
            val expected = dependantAbility.dependencyType == DependantAbility.DependencyType.REGULAR
            hasAbility = hasAbility && (dependencyEnabled == expected)
        }

        return hasAbility
    }

    fun interface AbilityRunner {
        fun run(player: Player)
    }

    fun interface AsyncAbilityRunner {
        suspend fun run(player: Player)
    }
}
