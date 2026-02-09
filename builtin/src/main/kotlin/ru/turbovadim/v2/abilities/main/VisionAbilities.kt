package ru.turbovadim.v2.abilities.main

import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

// ============================================
// VISION ABILITIES
// ============================================

/**
 * Cat Vision - night vision when not in water.
 * Legacy: CatVision.kt
 *
 * The legacy implementation:
 * - Stores existing non-infinite night vision effects
 * - Applies infinite night vision when NOT underwater
 * - Restores original effects when entering water
 * - Handles milk bucket consumption to clear stored effects
 */
val catVision = ability("cat_vision") {
    title = text("Nocturnal")
    description("You can slightly see in the dark when not in water.")

    onTick(interval = 1) { player, _ ->
        if (!player.isUnderWater) {
            player.addPotionEffect(
                PotionEffect(
                    PotionEffectType.NIGHT_VISION,
                    80,
                    0,
                    false,
                    false
                )
            )
        } else {
            // When underwater, remove night vision from this ability
            if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                player.removePotionEffect(PotionEffectType.NIGHT_VISION)
            }
        }
        true
    }
}

/**
 * Water Vision - night vision when underwater.
 * Legacy: WaterVision.kt
 *
 * The legacy implementation:
 * - Stores existing non-infinite night vision effects
 * - Applies infinite night vision when underwater
 * - Restores original effects when leaving water
 * - Handles milk bucket consumption to clear stored effects
 */
val waterVision = ability("water_vision") {
    title = text("Wet Eyes")
    description("Your vision underwater is perfect.")

    // Night vision ONLY when underwater
    // Uses finite duration (400 ticks / 20 sec) that gets refreshed each tick
    // This ensures the effect naturally expires when the ability is removed (e.g., origin change)
    onTick(interval = 1) { player, _ ->
        if (player.isUnderWater) {
            player.addPotionEffect(
                PotionEffect(
                    PotionEffectType.NIGHT_VISION,
                    400, // Long enough to avoid flickering, short enough to expire on ability removal
                    0,
                    false,
                    false
                )
            )
        } else {
            // Remove night vision when not underwater
            if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                player.removePotionEffect(PotionEffectType.NIGHT_VISION)
            }
        }
        true
    }
}

/**
 * Hotblooded - immune to poison and hunger status effects.
 * Legacy: Hotblooded.kt
 *
 * The legacy implementation cancels EntityPotionEffectEvent when
 * the new effect type is POISON or HUNGER.
 * Note: This needs event-based handling in the executor.
 */
val hotblooded = ability("hotblooded") {
    title = text("Hotblooded")
    description("Due to your hot body, venoms burn up, making you immune to poison and hunger status effects.")

    // Periodic check to remove poison and hunger effects
    // Note: Full implementation requires EntityPotionEffectEvent cancellation
    // This tick handler provides a fallback removal mechanism
    onTick(interval = 5) { player, _ ->
        // Remove poison effect if present
        if (player.hasPotionEffect(PotionEffectType.POISON)) {
            player.removePotionEffect(PotionEffectType.POISON)
        }
        // Remove hunger effect if present
        if (player.hasPotionEffect(PotionEffectType.HUNGER)) {
            player.removePotionEffect(PotionEffectType.HUNGER)
        }
        true
    }
}

/**
 * Slow Falling - has slow falling effect unless sneaking.
 * Legacy: SlowFalling.kt
 *
 * The legacy implementation uses packet events (PLAYER_POSITION) to check
 * sneaking state and applies/removes slow falling effect accordingly.
 * Uses infinite duration for the effect.
 */
val slowFalling = ability("slow_falling") {
    title = text("Featherweight")
    description("You fall as gently to the ground as a feather would, unless you sneak.")

    // Apply/remove slow falling based on sneaking state
    // Uses finite duration (40 ticks) that gets refreshed each tick
    // This ensures the effect naturally expires when the ability is removed (e.g., origin change)
    onTick(interval = 1) { player, _ ->
        if (!player.isSneaking) {
            player.addPotionEffect(
                PotionEffect(
                    PotionEffectType.SLOW_FALLING,
                    40, // Short duration, refreshed every tick
                    0,
                    false,
                    false
                )
            )
        } else {
            if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
                player.removePotionEffect(PotionEffectType.SLOW_FALLING)
            }
        }
        true
    }
}

/**
 * Collection of all vision-related abilities.
 */
val visionAbilities = listOf(
    catVision,
    waterVision,
    hotblooded,
    slowFalling
)
