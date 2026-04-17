package ru.turbovadim.v2.abilities.mobs

import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.ability.PotionReactionResult
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Witch-related abilities for the Mobs module.
 */

/**
 * Witch Particles - emit witch particles.
 * Legacy: ParticleAbility that spawns WITCH particles every 4 ticks.
 * Note: Requires PacketEvents ParticleTypes.WITCH for particle spawning.
 */
val witchParticles = ability("witch_particles", "moborigins") {
    title = text("Witch Particles")
    description("You emit witch particles.")
    visible = false

    option("frequency", 4)

    // Implementation note: Uses particles() DSL function with PacketEvents
    // particles(interval = 4) { player, config ->
    //     // Spawn ParticleTypes.WITCH at player location
    // }
}

/**
 * Better Potions - potions last longer when you drink them.
 * Legacy: On EntityPotionEffectEvent with POTION_DRINK cause:
 *   - Cancels the original event
 *   - Applies the same effect with 2x duration manually
 */
val betterPotions = ability("better_potions", "moborigins") {
    title = text("Better Potions")
    description("You consume potions better than most, Potions will last longer when you drink them.")

    option("duration_multiplier", 2.0)

    onPotionConsume { player, potion, config ->
        val multiplier = config.getDouble("duration_multiplier", 2.0)
        val newDuration = (potion.duration * multiplier).toInt()
        PotionReactionResult.Modify(
            PotionEffect(potion.type, newDuration, potion.amplifier, potion.isAmbient, potion.hasParticles())
        )
    }
}

/**
 * Potion Action - get a contextual potion effect based on your situation.
 * Legacy: On PlayerLeftClickEvent with empty hand:
 *   - Plays ENTITY_WITCH_DRINK sound
 *   - Applies situational potion based on player state:
 *     - On fire -> FIRE_RESISTANCE
 *     - Falling (fallDistance >= 4) -> SLOW_FALLING
 *     - Underwater (isUnderWater) -> WATER_BREATHING
 *     - Default -> SPEED
 *   - Duration: 200 ticks, amplifier: 0
 */
val potionAction = ability("potion_action", "moborigins") {
    title = text("Perfect Potion")
    description("Get a random potion effect, based on the situation you are in.")

    option("cooldown_ticks", 600)
    option("effect_duration", 200)

    onPrimaryAction { player, config ->
        if (!player.inventory.itemInMainHand.type.isAir) return@onPrimaryAction

        val duration = config.getInt("effect_duration", 200)

        // Play witch drinking sound
        player.world.playSound(player, Sound.ENTITY_WITCH_DRINK, SoundCategory.VOICE, 1f, 1f)

        // Determine contextual effect based on player's situation
        val effectType = when {
            // On fire -> Fire Resistance
            player.fireTicks > 0 -> PotionEffectType.FIRE_RESISTANCE

            // Falling a significant distance -> Slow Falling
            player.fallDistance >= 4 -> PotionEffectType.SLOW_FALLING

            // Underwater and running low on air -> Water Breathing
            // Note: Legacy uses NMSInvoker.isUnderWater, we use simplified check
            player.isInWater && player.remainingAir < player.maximumAir -> PotionEffectType.WATER_BREATHING

            // Default -> Speed
            else -> PotionEffectType.SPEED
        }

        player.addPotionEffect(PotionEffect(effectType, duration, 0))
    }
}

/**
 * Collection of all witch-related abilities.
 */
val witchAbilities = listOf(
    witchParticles,
    betterPotions,
    potionAction
)
