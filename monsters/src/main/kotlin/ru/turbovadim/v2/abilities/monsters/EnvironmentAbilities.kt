package ru.turbovadim.v2.abilities.monsters

import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityAirChangeEvent
import org.bukkit.event.entity.EntityExhaustionEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import kotlin.math.min

/**
 * Environment-related abilities for monster origins.
 */

val slowness = ability("slowness", "monsterorigins") {
    title = text("Zombie Slowness")
    description("Your undead body moves at a slower pace than humans.")

    attribute(
        AttributeType.MOVEMENT_SPEED,
        -0.15,
        operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        configKey = "speed_reduction"
    )
}

val landSlowness = ability("land_slowness", "monsterorigins") {
    title = text("Water Based")
    description("You're used to the water, so move much slower on land.")

    option("land_speed_reduction", -0.2)

    conditionalAttribute(
        type = AttributeType.MOVEMENT_SPEED,
        operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        checkInterval = 5,
        valueProvider = { player, config ->
            if (player.isInWater) 0.0 else config.getDouble("land_speed_reduction", -0.2)
        }
    )
}

val heatSlowness = ability("heat_slowness", "monsterorigins") {
    title = text("Cold Body")
    description("Your cold body conflicts with warmer biomes, slowing you down.")

    conditionalAttribute(
        type = AttributeType.MOVEMENT_SPEED,
        operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        checkInterval = 10,
        valueProvider = { player, _ ->
            val temp = player.location.block.temperature
            when {
                temp >= 2.0 -> -0.2
                temp >= 1.5 -> -0.15
                temp >= 1.0 -> -0.1
                temp >= 0.5 -> -0.05
                else -> 0.0
            }
        }
    )
}

val coldSlowness = ability("cold_slowness", "monsterorigins") {
    title = text("Warm Body")
    description("Your warm body conflicts with colder biomes, slowing you down.")

    conditionalAttribute(
        type = AttributeType.MOVEMENT_SPEED,
        operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        checkInterval = 10,
        valueProvider = { player, _ ->
            val temp = player.location.block.temperature
            when {
                temp <= 0.0 -> -0.2
                temp <= 0.5 -> -0.15
                temp <= 1.0 -> -0.1
                temp <= 1.5 -> -0.05
                else -> 0.0
            }
        }
    )
}

val waterBreathingMonsters = ability("water_breathing", "monsterorigins") {
    title = text("Water Breathing")
    description("You can breathe underwater.")
    visible = false

    listener<EntityAirChangeEvent>(
        ignoreCancelled = false,
        playerFrom = { it.entity as? Player }
    ) { player, event, _ ->
        event.amount = player.maximumAir
    }
}

val swimSpeedMonsters = ability("swim_speed", "monsterorigins") {
    title = text("Fast Swimmer")
    description("Your underwater speed is increased.")

    onTick(interval = 1) { player, _ ->
        if (OriginsReforged.NMSInvoker.isUnderWater(player)) {
            val effect = player.getPotionEffect(PotionEffectType.DOLPHINS_GRACE)
            val ambient = effect?.isAmbient ?: false
            val showParticles = effect?.hasParticles() ?: false
            player.addPotionEffect(
                PotionEffect(
                    PotionEffectType.DOLPHINS_GRACE,
                    Int.MAX_VALUE,
                    -1,
                    ambient,
                    showParticles
                )
            )
        } else {
            val effect = player.getPotionEffect(PotionEffectType.DOLPHINS_GRACE)
            if (effect != null && effect.amplifier == -1) {
                player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE)
            }
        }
        true
    }
}

val zombieHunger = ability("zombie_hunger", "monsterorigins") {
    title = text("Zombie Hunger")
    description("Your constant hunger for flesh makes you exhaust quicker than a human.")

    option("exhaustion_multiplier", 1.5f)

    listener<EntityExhaustionEvent>(
        ignoreCancelled = false,
        playerFrom = { it.entity as? Player }
    ) { _, event, config ->
        val multiplier = config.getFloat("exhaustion_multiplier", 1.5f)
        event.exhaustion *= multiplier
    }
}

val halfMaxSaturation = ability("half_max_saturation", "monsterorigins") {
    title = text("Poor Digestion")
    description("You can only hold half as much saturation as a human.")

    listener<FoodLevelChangeEvent>(
        ignoreCancelled = false,
        playerFrom = { it.entity as? Player }
    ) { player, _, _ ->
        player.saturation = min(player.saturation, player.foodLevel.toFloat() / 2f)
    }
}

val senseMovement = ability("sense_movement", "monsterorigins") {
    title = text("Heightened Senses")
    description("You can see the outlines of nearby mobs, even through blocks.")

    option("detection_radius", 24.0)
    option("glow_radius", 16.0)

    onTick(interval = 20) { player, config ->
        val detectionRadius = config.getDouble("detection_radius", 24.0)
        val glowRadius = config.getDouble("glow_radius", 16.0)

        for (entity in player.getNearbyEntities(detectionRadius, detectionRadius, detectionRadius)) {
            if (entity === player) continue
            if (entity !is LivingEntity) continue

            var data: Byte = 0
            if (entity.isGlowing || entity.location.distance(player.location) <= glowRadius) {
                data = (data + 0x40).toByte()
            }
            if (entity.fireTicks > 0) {
                data = (data + 0x01).toByte()
            }
            if (entity.isInvisible) {
                data = (data + 0x20).toByte()
            }
            if (entity is Player) {
                if (entity.isSneaking) {
                    data = (data + 0x02).toByte()
                }
                if (entity.isSprinting) {
                    data = (data + 0x08).toByte()
                }
                if (entity.isSwimming) {
                    data = (data + 0x10).toByte()
                }
                if (entity.isGliding) {
                    data = (data + 0x80.toByte()).toByte()
                }
            }

            OriginsReforged.NMSInvoker.sendEntityData(player, entity, data)
        }
        true
    }
}
