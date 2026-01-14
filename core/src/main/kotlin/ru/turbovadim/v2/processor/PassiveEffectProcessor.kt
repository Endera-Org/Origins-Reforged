package ru.turbovadim.v2.processor

import net.kyori.adventure.key.Key
import org.bukkit.GameMode
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import ru.turbovadim.v2.ability.AbilityEffect
import ru.turbovadim.v2.ability.FallDamageMode
import ru.turbovadim.v2.ability.InvisibilityCondition
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.state.PlayerOriginState

/**
 * Processor for passive ability effects.
 *
 * Passive effects are applied ONCE when origins change, not periodically.
 * This includes:
 * - Attribute modifiers (from config)
 * - Flight capability
 * - Visibility
 *
 * This replaces the `updateAllPlayers()` polling loop with event-driven updates.
 */
class PassiveEffectProcessor(private val container: OriginsContainer) {

    companion object {
        // Namespace for origin-related attribute modifiers
        private const val MODIFIER_NAMESPACE = "origins"
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
     * Apply attribute modifiers from config.
     */
    private fun applyAttributes(player: Player, state: PlayerOriginState) {
        // Clear existing origin attributes first
        clearOriginAttributes(player)

        // Get all abilities for the player
        val abilityKeys = state.getAbilityKeys()

        // Apply attributes from config for each ability
        for (abilityKey in abilityKeys) {
            val attributes = container.configLoader.getAttributes(abilityKey)

            for (attr in attributes) {
                applyAttributeModifier(player, abilityKey, attr.attribute, attr.value, attr.operation)
            }
        }
    }

    /**
     * Apply a single attribute modifier using NMSInvoker for cross-version compatibility.
     */
    private fun applyAttributeModifier(
        player: Player,
        abilityKey: Key,
        attribute: Attribute,
        value: Double,
        operation: AttributeModifier.Operation
    ) {
        val playerAttribute = player.getAttribute(attribute) ?: return

        val modifierKey = NamespacedKey(
            container.plugin,
            "${MODIFIER_NAMESPACE}_${abilityKey.value()}_${attribute.name.lowercase()}"
        )

        val modifierName = "${abilityKey.value()}_${attribute.name.lowercase()}"

        // Remove existing modifier with same key (if any)
        val existing = container.nmsInvoker.getAttributeModifier(playerAttribute, modifierKey)
        if (existing != null) {
            playerAttribute.removeModifier(existing)
        }

        // Add new modifier using NMSInvoker
        container.nmsInvoker.addAttributeModifier(
            playerAttribute,
            modifierKey,
            modifierName,
            value,
            operation
        )
    }

    /**
     * Clear all origin-related attribute modifiers from a player.
     */
    private fun clearOriginAttributes(player: Player) {
        for (attribute in Attribute.entries) {
            val playerAttr = player.getAttribute(attribute) ?: continue

            // Remove all modifiers that match our naming pattern
            // We iterate through a copy to avoid concurrent modification
            val toRemove = playerAttr.modifiers.filter { modifier ->
                modifier.name.startsWith(MODIFIER_NAMESPACE)
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

        val activeFlightAbilities = flightAbilityKeys.intersect(playerAbilities)

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

        val activeInvisAbilities = invisAbilityKeys.intersect(playerAbilities)

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
}
