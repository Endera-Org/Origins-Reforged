package ru.turbovadim.v2.abilities.mobs

import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes
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
 */
val witchParticles = ability("witch_particles", "moborigins") {
    title = text("Witch Particles")
    description("You emit witch particles.")
    visible = false

    particles(
        particleType = ParticleTypes.WITCH,
        frequency = 4,
        offsetX = 0.3f,
        offsetY = 0.5f,
        offsetZ = 0.3f,
        count = 1
    )
}

/**
 * Better Potions - potions last longer when you drink them.
 */
val betterPotions = ability("better_potions", "moborigins") {
    title = text("Better Potions")
    description("You consume potions better than most, Potions will last longer when you drink them.")

    option("duration_multiplier", 2.0)

    onPotionConsume { _, potion, config ->
        val multiplier = config.getDouble("duration_multiplier", 2.0)
        val newDuration = (potion.duration * multiplier).toInt()
        PotionReactionResult.Modify(
            PotionEffect(potion.type, newDuration, potion.amplifier, potion.isAmbient, potion.hasParticles())
        )
    }
}

/**
 * Potion Action - get a contextual potion effect based on your situation.
 */
val potionAction = ability("potion_action", "moborigins") {
    title = text("Perfect Potion")
    description("Get a random potion effect, based on the situation you are in.")

    option("cooldown_ticks", 600)
    option("effect_duration", 200)

    onPrimaryAction { player, config ->
        if (!player.inventory.itemInMainHand.type.isAir) return@onPrimaryAction

        val api = ru.turbovadim.v2.api.OriginsApi.getOrNull()
        val abilityKey = net.kyori.adventure.key.Key.key("moborigins", "potion_action")
        if (api != null && api.hasCooldown(player, abilityKey)) return@onPrimaryAction

        val duration = config.getInt("effect_duration", 200)

        player.world.playSound(player, Sound.ENTITY_WITCH_DRINK, SoundCategory.VOICE, 1f, 1f)

        val effectType = when {
            player.fireTicks > 0 -> PotionEffectType.FIRE_RESISTANCE
            player.fallDistance >= 4 -> PotionEffectType.SLOW_FALLING
            player.isInWater && player.remainingAir < player.maximumAir -> PotionEffectType.WATER_BREATHING
            else -> PotionEffectType.SPEED
        }

        player.addPotionEffect(PotionEffect(effectType, duration, 0))
        api?.setCooldown(player, abilityKey, config.getInt("cooldown_ticks", 600), "potion")
    }
}

val witchAbilities = listOf(
    witchParticles,
    betterPotions,
    potionAction
)
