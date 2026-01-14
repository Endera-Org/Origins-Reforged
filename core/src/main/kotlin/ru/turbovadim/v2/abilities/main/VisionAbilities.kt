package ru.turbovadim.v2.abilities.main

import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.dsl.*

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

    option("effect_duration", -1) // -1 means infinite in Paper

    // Night vision when NOT underwater
    // The executor should handle effect storing/restoring logic
    onTick(interval = 1) { player, _ ->
        if (!player.isUnderWater) {
            // Apply infinite night vision
            // Legacy uses infiniteDuration() which returns -1 for infinite
            val currentEffect = player.getPotionEffect(PotionEffectType.NIGHT_VISION)
            val ambient = currentEffect?.isAmbient == true
            val showParticles = currentEffect?.hasParticles() == true

            // Only apply if not already having infinite night vision
            if (currentEffect == null || currentEffect.duration != -1) {
                player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.NIGHT_VISION,
                        -1, // Infinite duration
                        0,
                        ambient,
                        showParticles
                    )
                )
            }
        } else {
            // When underwater, remove infinite night vision
            player.getPotionEffect(PotionEffectType.NIGHT_VISION)?.let { effect ->
                if (effect.duration == -1) {
                    player.removePotionEffect(PotionEffectType.NIGHT_VISION)
                }
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

    option("effect_duration", -1)

    // Night vision ONLY when underwater
    onTick(interval = 1) { player, _ ->
        if (player.isUnderWater) {
            val currentEffect = player.getPotionEffect(PotionEffectType.NIGHT_VISION)
            val ambient = currentEffect?.isAmbient == true
            val showParticles = currentEffect?.hasParticles() == true

            // Apply infinite night vision when underwater
            if (currentEffect == null || currentEffect.duration != -1) {
                player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.NIGHT_VISION,
                        -1,
                        0,
                        ambient,
                        showParticles
                    )
                )
            }
        } else {
            // Remove infinite night vision when not underwater
            player.getPotionEffect(PotionEffectType.NIGHT_VISION)?.let { effect ->
                if (effect.duration == -1) {
                    player.removePotionEffect(PotionEffectType.NIGHT_VISION)
                }
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

    option("effect_duration", -1)

    // Apply/remove slow falling based on sneaking state
    onTick(interval = 1) { player, _ ->
        if (!player.isSneaking) {
            // Apply slow falling if not already present
            if (!player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
                player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.SLOW_FALLING,
                        -1, // Infinite duration
                        0,
                        false,
                        false
                    )
                )
            }
        } else {
            // Remove slow falling when sneaking
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
