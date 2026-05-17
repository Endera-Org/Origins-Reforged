package ru.turbovadim.v2.processor

import net.kyori.adventure.key.Key
import org.bukkit.GameMode
import org.bukkit.entity.Player
import ru.turbovadim.v2.ability.FallDamageMode
import ru.turbovadim.v2.ability.InvisibilityCondition
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.state.PlayerOriginState

/**
 * Processor for passive ability effects.
 *
 * Passive effects are applied ONCE when origins change, not periodically.
 * This includes:
 * - Attribute modifiers
 * - Flight capability
 * - Visibility
 *
 * This replaces the `updateAllPlayers()` polling loop with event-driven updates.
 */
class PassiveEffectProcessor(private val container: OriginsContainer) {

    companion object {
        private const val REMOVED_CONFIG_ATTRIBUTE_NAMESPACE = "origins"
    }

    /**
     * Apply all passive effects for a player.
     * Called when their origins change.
     */
    fun applyPassiveEffects(player: Player, state: PlayerOriginState) {
        applyAttributes(player, state)
        applyFlight(player, state)
        applyVisibility(player, state)
    }

    /**
     * Remove all passive effects from a player.
     * Called when they lose all origins.
     */
    fun removePassiveEffects(player: Player) {
        clearOriginAttributes(player)
        resetFlight(player)
        player.isInvisible = false
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

    /**
     * Apply flight capability.
     */
    private fun applyFlight(player: Player, state: PlayerOriginState) {
        val flightAbilityKeys = container.abilityRegistry.getFlightAbilities()
        val playerAbilities = state.getAbilityKeys()

        // Filter to abilities the player has AND whose dependencies are satisfied
        val activeFlightAbilities = flightAbilityKeys
            .intersect(playerAbilities)
            .filter { isDependencySatisfied(player, it) }

        if (activeFlightAbilities.isEmpty()) {
            // No flight abilities - reset to game mode defaults
            resetFlight(player)
            return
        }

        // Get all active flight effects
        val flightEffects = activeFlightAbilities
            .mapNotNull { container.abilityRegistry.getFlightEffect(it) }

        if (flightEffects.isEmpty()) {
            resetFlight(player)
            return
        }

        // Use minimum speed among all flight abilities
        val minSpeed = flightEffects.minOf { it.speed }

        // Check fall damage mode - if any ability prevents fall damage, prevent it
        val preventFallDamage = flightEffects.any { it.fallDamage == FallDamageMode.NONE }

        player.allowFlight = true
        player.flySpeed = minSpeed.coerceIn(0.0001f, 1.0f)

        // Store fall damage mode in player state for damage handler
        state.setAbilityState(
            Key.key("origins", "_flight_fall_damage"),
            if (preventFallDamage) FallDamageMode.NONE else FallDamageMode.NORMAL
        )
    }

    /**
     * Reset flight to game mode defaults.
     */
    private fun resetFlight(player: Player) {
        when (player.gameMode) {
            GameMode.CREATIVE, GameMode.SPECTATOR -> {
                player.allowFlight = true
                player.flySpeed = 0.1f
            }
            else -> {
                player.allowFlight = false
                player.isFlying = false
                player.flySpeed = 0.1f
            }
        }
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
