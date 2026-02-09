package ru.turbovadim.v2.api

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import ru.turbovadim.v2.ability.Ability
import ru.turbovadim.v2.ability.AbilityCheckResult
import ru.turbovadim.v2.ability.DependencyAbility
import ru.turbovadim.v2.addon.AbilityCheckHook
import ru.turbovadim.v2.addon.ResourcePackInfo
import ru.turbovadim.v2.event.OriginChangeReason
import ru.turbovadim.v2.origin.Origin

/**
 * Public API facade for the Origins system.
 *
 * Addon developers use this interface to interact with the Origins system
 * without depending on internal implementations.
 *
 * Obtain the instance via [OriginsApi.get] or [OriginsApi.getOrNull].
 */
interface OriginsApi {

    // ========== Registration ==========

    /** Register an ability. Also generates its default config. */
    fun registerAbility(ability: Ability)

    /** Register an origin. */
    fun registerOrigin(origin: Origin)

    /** Register an addon ability check hook. */
    fun registerAbilityCheckHook(hook: AbilityCheckHook)

    /** Register a resource pack from an addon. */
    fun registerResourcePack(namespace: String, packInfo: ResourcePackInfo)

    // ========== Ability queries ==========

    /** Get an ability by key, or null. */
    fun getAbility(key: Key): Ability?

    /** Get all registered abilities. */
    fun getAllAbilities(): Collection<Ability>

    /** Get a dependency ability by key, or null. */
    fun getDependencyAbility(key: Key): DependencyAbility?

    // ========== Origin queries ==========

    /** Get an origin by key, or null. */
    fun getOrigin(key: Key): Origin?

    /** Get an origin by name (case-insensitive), or null. */
    fun getOriginByName(name: String): Origin?

    /** Get all origins for a layer, sorted by position. */
    fun getOriginsByLayer(layer: String): List<Origin>

    /** Get all registered layers. */
    fun getLayers(): List<String>

    // ========== Player origin state ==========

    /** Get the player's current origin for a layer, or null. */
    fun getPlayerOrigin(player: Player, layer: String): Origin?

    /** Check if a player has a specific ability. */
    fun hasAbility(player: Player, key: Key): Boolean

    /** Get all ability keys the player currently has. */
    fun getPlayerAbilityKeys(player: Player): Set<Key>

    /** Set a player's origin for a layer. */
    fun setPlayerOrigin(
        player: Player,
        layer: String,
        origin: Origin,
        reason: OriginChangeReason = OriginChangeReason.PLUGIN
    )

    /** Remove a player's origin from a layer. */
    fun removePlayerOrigin(
        player: Player,
        layer: String,
        reason: OriginChangeReason = OriginChangeReason.PLUGIN
    ): Origin?

    // ========== Ability state ==========

    /** Get a typed state value for a player. */
    fun <T : Any> getState(player: Player, key: ru.turbovadim.v2.ability.StateKey<T>): T

    /** Set a typed state value for a player. */
    fun <T : Any> setState(player: Player, key: ru.turbovadim.v2.ability.StateKey<T>, value: T)

    /** Reset a typed state to its default for a player. */
    fun <T : Any> resetState(player: Player, key: ru.turbovadim.v2.ability.StateKey<T>)

    // ========== Passive effects / dependency lifecycle ==========

    /** Re-apply passive effects for a player (flight, invisibility, attributes). */
    fun reapplyPassiveEffects(player: Player)

    /** Trigger dependency lifecycle callbacks for abilities depending on the given key. */
    fun triggerDependencyLifecycle(player: Player, dependencyKey: Key, enabled: Boolean)

    // ========== Cooldowns ==========

    /** Set a cooldown on an ability for a player. */
    fun setCooldown(player: Player, abilityKey: Key, durationTicks: Int, icon: String? = null)

    /** Check if a player has a cooldown on an ability. */
    fun hasCooldown(player: Player, abilityKey: Key): Boolean

    /** Get remaining cooldown ticks for an ability, or 0 if no cooldown. */
    fun getCooldownTicks(player: Player, abilityKey: Key): Int

    companion object {
        @Volatile
        private var instance: OriginsApi? = null

        /**
         * Get the current API instance.
         * @throws IllegalStateException if the API is not yet initialized
         */
        fun get(): OriginsApi {
            return instance ?: throw IllegalStateException(
                "OriginsApi not initialized. Is the Origins-Reforged plugin enabled?"
            )
        }

        /**
         * Get the current API instance, or null if not initialized.
         */
        fun getOrNull(): OriginsApi? = instance

        /**
         * Register the API implementation. Called internally by the core plugin.
         */
        fun register(api: OriginsApi) {
            instance = api
        }

        /**
         * Unregister the API implementation. Called internally on plugin disable.
         */
        fun unregister() {
            instance = null
        }
    }
}
