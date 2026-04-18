package ru.turbovadim.v2.api

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.endera.enderalib.utils.async.runTask
import ru.turbovadim.OriginsReforged
import ru.turbovadim.PackApplier
import ru.turbovadim.packetsenders.OriginsReforgedResourcePackInfo
import ru.turbovadim.v2.ability.Ability
import ru.turbovadim.v2.ability.AbilityEffect
import ru.turbovadim.v2.ability.DependencyAbility
import ru.turbovadim.v2.ability.StateKey
import ru.turbovadim.v2.addon.AbilityCheckHook
import ru.turbovadim.v2.addon.ResourcePackInfo
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.event.OriginChangeReason
import ru.turbovadim.v2.origin.Origin
import java.io.File
import java.net.URI
import net.kyori.adventure.resource.ResourcePackInfo as AdventureResourcePackInfo

/**
 * Implementation of [OriginsApi] that delegates to [OriginsContainer] services.
 */
class OriginsApiImpl(private val container: OriginsContainer) : OriginsApi {

    // ========== Registration ==========

    override fun registerAbility(ability: Ability) {
        container.abilityRegistry.register(ability)
        container.configLoader.registerDefaults(ability.key, ability.defaultOptions)
    }

    override fun registerOrigin(origin: Origin) {
        container.originRegistry.register(origin)
    }

    override fun registerAbilityCheckHook(hook: AbilityCheckHook) {
        container.addonAbilityCheckRegistry.register(hook)
    }

    override fun registerResourcePack(namespace: String, packInfo: ResourcePackInfo) {
        try {
            val adventurePackInfo = AdventureResourcePackInfo.resourcePackInfo()
                .uri(URI.create(packInfo.url))
                .hash(packInfo.hash)
                .build()
            PackApplier.addResourcePack(namespace, OriginsReforgedResourcePackInfo(adventurePackInfo))
        } catch (e: Exception) {
            container.plugin.logger.warning("Failed to register resource pack: ${e.message}")
        }
    }

    override fun loadBundledOrigins(
        addonId: String,
        folderName: String,
        jarFile: File?,
        dataFolder: File?
    ) {
        val plugin = container.plugin
        val effectiveJar = jarFile ?: (plugin as? OriginsReforged)?.file
        if (effectiveJar == null) {
            plugin.logger.warning("Failed to load bundled origins for '$addonId': no jar file provided")
            return
        }
        container.originLoader.loadOriginsForAddon(
            addonId = addonId,
            dataFolder = dataFolder ?: plugin.dataFolder,
            jarFile = effectiveJar,
            folderName = folderName
        )
    }

    // ========== Ability queries ==========

    override fun getAbility(key: Key): Ability? {
        return container.abilityRegistry.get(key)
    }

    override fun getAllAbilities(): Collection<Ability> {
        return container.abilityRegistry.getAll()
    }

    override fun getDependencyAbility(key: Key): DependencyAbility? {
        return container.abilityRegistry.getDependencyAbility(key)
    }

    // ========== Origin queries ==========

    override fun getOrigin(key: Key): Origin? {
        return container.originRegistry.get(key)
    }

    override fun getOriginByName(name: String): Origin? {
        return container.originRegistry.getByName(name)
    }

    override fun getOriginsByLayer(layer: String): List<Origin> {
        return container.originRegistry.getByLayer(layer)
    }

    override fun getLayers(): List<String> {
        return container.originRegistry.layers
    }

    // ========== Player origin state ==========

    override fun getPlayerOrigin(player: Player, layer: String): Origin? {
        val state = container.playerStateManager.getStateOrNull(player) ?: return null
        return state.getOrigin(layer)
    }

    override fun hasAbility(player: Player, key: Key): Boolean {
        val state = container.playerStateManager.getStateOrNull(player) ?: return false
        return state.getAbilityKeys().contains(key)
    }

    override fun getPlayerAbilityKeys(player: Player): Set<Key> {
        val state = container.playerStateManager.getStateOrNull(player) ?: return emptySet()
        return state.getAbilityKeys()
    }

    override fun setPlayerOrigin(
        player: Player,
        layer: String,
        origin: Origin,
        reason: OriginChangeReason
    ) {
        container.playerStateManager.setOrigin(player, layer, origin, reason)
    }

    override fun removePlayerOrigin(
        player: Player,
        layer: String,
        reason: OriginChangeReason
    ): Origin? {
        return container.playerStateManager.removeOrigin(player, layer, reason)
    }

    // ========== Ability state ==========

    override fun <T : Any> getState(player: Player, key: StateKey<T>): T {
        val state = container.playerStateManager.getState(player)
        return state.getTypedState(key)
    }

    override fun <T : Any> setState(player: Player, key: StateKey<T>, value: T) {
        val state = container.playerStateManager.getState(player)
        state.setTypedState(key, value)
    }

    override fun <T : Any> resetState(player: Player, key: StateKey<T>) {
        val state = container.playerStateManager.getState(player)
        state.removeTypedState(key)
    }

    // ========== Passive effects / dependency lifecycle ==========

    /**
     * Reapply all passive effects for a player.
     *
     * Folia-safe: hops work onto the player's own entity scheduler. API callers
     * can invoke this from any thread (event handler, async task, command). Note
     * that effects are applied on the player's next region tick, not inline.
     */
    override fun reapplyPassiveEffects(player: Player) {
        val state = container.playerStateManager.getStateOrNull(player) ?: return
        player.runTask(container.plugin) {
            container.passiveEffectProcessor.applyPassiveEffects(player, state)
        }
    }

    /**
     * Trigger dependency-lifecycle callbacks for a player.
     *
     * Folia-safe: user-supplied handlers run on the player's own region thread.
     * Callable from any thread; handlers fire on the player's next region tick.
     */
    override fun triggerDependencyLifecycle(player: Player, dependencyKey: Key, enabled: Boolean) {
        val state = container.playerStateManager.getStateOrNull(player) ?: return
        val playerAbilities = state.getAbilityKeys()

        player.runTask(container.plugin) {
            for (abilityKey in playerAbilities) {
                val ability = container.abilityRegistry.get(abilityKey) ?: continue
                if (ability.dependencyKey != dependencyKey) continue

                val config = container.configLoader.getAccessor(abilityKey, ability.defaultOptions)

                for (effect in ability.effects) {
                    when {
                        enabled && effect is AbilityEffect.Lifecycle.OnDependencyEnabled -> {
                            effect.handler.onStateChange(player, config)
                        }
                        !enabled && effect is AbilityEffect.Lifecycle.OnDependencyDisabled -> {
                            effect.handler.onStateChange(player, config)
                        }
                    }
                }
            }
        }
    }

    // ========== Cooldowns ==========

    override fun setCooldown(player: Player, abilityKey: Key, durationTicks: Int, icon: String?) {
        container.cooldownManager.setCooldown(player, abilityKey, durationTicks, icon)
    }

    override fun hasCooldown(player: Player, abilityKey: Key): Boolean {
        return container.cooldownManager.hasCooldown(player, abilityKey)
    }

    override fun getCooldownTicks(player: Player, abilityKey: Key): Int {
        return container.cooldownManager.getCooldown(player, abilityKey)
    }
}
