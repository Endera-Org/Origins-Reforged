package ru.turbovadim.v2.processor

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import ru.turbovadim.v2.ability.FallDamageMode
import ru.turbovadim.v2.ability.InvisibilityCondition
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.state.PlayerOriginState
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Processor for passive ability effects.
 *
 * Most passive effects are applied when origins change. Conditional flight is
 * additionally refreshed by [PeriodicAbilityProcessor] every tick.
 *
 * This includes:
 * - Attribute modifiers
 * - Flight capability
 * - Visibility
 */
class PassiveEffectProcessor(private val container: OriginsContainer) {

    companion object {
        private const val REMOVED_CONFIG_ATTRIBUTE_NAMESPACE = "origins"
        private val FLIGHT_FALL_DAMAGE_KEY = Key.key("origins", "_flight_fall_damage")
    }

    private data class FlightState(
        val previousAllowFlight: Boolean,
        val previousFlying: Boolean,
        val previousFlySpeed: Float
    )

    private val flightStates = ConcurrentHashMap<UUID, FlightState>()

    /**
     * Apply all passive effects for a player.
     * Called when their origins change.
     */
    fun applyPassiveEffects(player: Player, state: PlayerOriginState) {
        applyAttributes(player, state)
        refreshFlight(player, state)
        applyVisibility(player, state)
    }

    /**
     * Remove all passive effects from a player.
     * Called when they lose all origins.
     */
    fun removePassiveEffects(player: Player) {
        clearOriginAttributes(player)
        releaseFlight(player)
        player.isInvisible = false
    }

    /**
     * Re-evaluate all flight effects owned by the player and apply one combined
     * result. This is called once per tick for players with a flight effect.
     */
    fun refreshFlight(player: Player) {
        val state = container.playerStateManager.getStateOrNull(player)
        if (state == null) {
            releaseFlight(player)
            return
        }
        refreshFlight(player, state)
    }

    /**
     * Forget captured state for a disconnected player without mutating Bukkit
     * state after the player has left.
     */
    fun removePlayer(playerId: UUID) {
        flightStates.remove(playerId)
    }

    /** Restore flight state for online players before the plugin shuts down. */
    fun shutdown() {
        Bukkit.getOnlinePlayers().forEach(::releaseFlight)
        flightStates.clear()
    }

    /**
     * Apply static attribute modifiers declared by abilities.
     */
    private fun applyAttributes(player: Player, state: PlayerOriginState) {
        // Clear the removed config-based attribute namespace so stale modifiers
        // from older builds are removed the next time passives are reapplied.
        clearOriginAttributes(player)

        container.attributeAbilityProcessor.clearStaticAttributes(player)
        container.attributeAbilityProcessor.applyStaticAttributes(player, state)
    }

    /**
     * Clear all origin-related attribute modifiers from a player.
     * Uses NMSInvoker attributes for cross-version compatibility.
     */
    private fun clearOriginAttributes(player: Player) {
        // Get all known attributes from NMSInvoker (version-compatible)
        val nms = container.nmsInvoker
        val attributes = listOfNotNull(
            nms.armorAttribute,
            nms.maxHealthAttribute,
            nms.movementSpeedAttribute,
            nms.flyingSpeedAttribute,
            nms.attackDamageAttribute,
            nms.attackKnockbackAttribute,
            nms.attackSpeedAttribute,
            nms.armorToughnessAttribute,
            nms.luckAttribute,
            nms.knockbackResistanceAttribute,
            nms.followRangeAttribute,
            nms.fallDamageMultiplierAttribute,
            nms.maxAbsorptionAttribute,
            nms.safeFallDistanceAttribute,
            nms.scaleAttribute,
            nms.stepHeightAttribute,
            nms.gravityAttribute,
            nms.jumpStrengthAttribute,
            nms.burningTimeAttribute,
            nms.explosionKnockbackResistanceAttribute,
            nms.movementEfficiencyAttribute,
            nms.oxygenBonusAttribute,
            nms.waterMovementEfficiencyAttribute,
            nms.blockInteractionRangeAttribute,
            nms.entityInteractionRangeAttribute,
            nms.blockBreakSpeedAttribute,
            nms.miningEfficiencyAttribute,
            nms.sneakingSpeedAttribute,
            nms.submergedMiningSpeedAttribute,
            nms.sweepingDamageRatioAttribute
        )

        for (attribute in attributes) {
            val playerAttr = player.getAttribute(attribute) ?: continue

            // Remove all modifiers that match our naming pattern
            // We iterate through a copy to avoid concurrent modification
            val toRemove = playerAttr.modifiers.filter { modifier ->
                modifier.name.startsWith(REMOVED_CONFIG_ATTRIBUTE_NAMESPACE)
            }

            for (modifier in toRemove) {
                playerAttr.removeModifier(modifier)
            }
        }
    }

    private fun refreshFlight(player: Player, state: PlayerOriginState) {
        val flightAbilityKeys = container.abilityRegistry.getFlightAbilities()
        val playerAbilities = state.getAbilityKeys()

        val activeFlightEffects = flightAbilityKeys
            .intersect(playerAbilities)
            .filter { isDependencySatisfied(player, it) }
            .mapNotNull { abilityKey ->
                val ability = container.abilityRegistry.get(abilityKey) ?: return@mapNotNull null
                val effect = container.abilityRegistry.getFlightEffect(abilityKey) ?: return@mapNotNull null
                val config = container.configLoader.getAccessor(abilityKey, ability.defaultOptions)
                effect.takeIf { it.condition.check(player, config) }
            }

        if (activeFlightEffects.isEmpty()) {
            releaseFlight(player)
            return
        }

        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) return

        flightStates.computeIfAbsent(player.uniqueId) {
            FlightState(
                previousAllowFlight = player.allowFlight,
                previousFlying = player.isFlying,
                previousFlySpeed = player.flySpeed
            )
        }

        player.allowFlight = true
        player.flySpeed = activeFlightEffects.minOf { it.speed }.coerceIn(0.0001f, 1.0f)

        val fallDamageMode = when {
            activeFlightEffects.any { it.fallDamage == FallDamageMode.NONE } -> FallDamageMode.NONE
            activeFlightEffects.any { it.fallDamage == FallDamageMode.REDUCED } -> FallDamageMode.REDUCED
            else -> FallDamageMode.NORMAL
        }

        if (fallDamageMode == FallDamageMode.NONE) {
            player.fallDistance = 0f
        }

        state.setAbilityState(FLIGHT_FALL_DAMAGE_KEY, fallDamageMode)
    }

    private fun releaseFlight(player: Player) {
        container.playerStateManager.getStateOrNull(player)?.removeAbilityState(FLIGHT_FALL_DAMAGE_KEY)
        val previous = flightStates.remove(player.uniqueId) ?: return
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) return

        player.allowFlight = previous.previousAllowFlight
        player.isFlying = previous.previousAllowFlight && previous.previousFlying
        player.flySpeed = previous.previousFlySpeed.coerceIn(-1.0f, 1.0f)
    }

    /**
     * Apply visibility effects.
     */
    private fun applyVisibility(player: Player, state: PlayerOriginState) {
        val invisAbilityKeys = container.abilityRegistry.getInvisibilityAbilities()
        val playerAbilities = state.getAbilityKeys()

        // Filter to abilities the player has AND whose dependencies are satisfied
        val activeInvisAbilities = invisAbilityKeys
            .intersect(playerAbilities)
            .filter { isDependencySatisfied(player, it) }

        if (activeInvisAbilities.isEmpty()) {
            player.isInvisible = false
            return
        }

        // Get all active invisibility effects
        val invisEffects = activeInvisAbilities
            .mapNotNull { container.abilityRegistry.getInvisibilityEffect(it) }

        if (invisEffects.isEmpty()) {
            player.isInvisible = false
            return
        }

        // Check if any effect grants unconditional invisibility
        val isInvisible = invisEffects.any { effect ->
            when (val condition = effect.condition) {
                is InvisibilityCondition.Always -> true
                is InvisibilityCondition.WhenSneaking -> player.isSneaking
                is InvisibilityCondition.Custom -> condition.check(player)
            }
        }

        player.isInvisible = isInvisible
    }

    /**
     * Check if an ability's dependency is satisfied.
     * Returns true if the ability has no dependency, or if the dependency ability is enabled/disabled as required.
     */
    private fun isDependencySatisfied(player: Player, abilityKey: Key): Boolean {
        val ability = container.abilityRegistry.get(abilityKey) ?: return true
        val depKey = ability.dependencyKey ?: return true

        val depAbility = container.abilityRegistry.getDependencyAbility(depKey) ?: return true
        val isDepEnabled = depAbility.isEnabled(player)

        // If dependencyInverse is true, the dependency must be DISABLED
        // If dependencyInverse is false, the dependency must be ENABLED
        return if (ability.dependencyInverse) !isDepEnabled else isDepEnabled
    }
}
