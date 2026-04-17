package ru.turbovadim.v2.abilities.fantasy

import io.papermc.paper.world.MoonPhase
import org.bukkit.World
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.EnderCrystal
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text
import kotlin.math.min

/**
 * Environment and dimension-related abilities for the Fantasy Origins module.
 */

val endBoost = ability("end_boost", "fantasyorigins") {
    title = text("End Inhabitant")
    description(
        "Your natural habitat is the end, so you have more health",
        "and are stronger when you are there."
    )

    option("damage_multiplier", 1.6)
    option("health_bonus", 20.0)

    conditionalAttributeWhen(
        type = AttributeType.ATTACK_DAMAGE,
        value = 1.6,
        operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        checkInterval = 20
    ) { player, config ->
        player.world.environment == World.Environment.THE_END
    }

    conditionalAttributeWhen(
        type = AttributeType.MAX_HEALTH,
        value = 20.0,
        operation = AttributeModifier.Operation.ADD_NUMBER,
        checkInterval = 20
    ) { player, config ->
        player.world.environment == World.Environment.THE_END
    }
}

val endCrystalHealing = ability("end_crystal_healing", "fantasyorigins") {
    title = text("Crystal Healer")
    description("You can regenerate health from nearby End Crystals.")

    option("search_radius", 20.0)
    option("max_distance", 12.0)
    option("heal_amount", 1.0)

    onTick(interval = 5) { player, config ->
        val searchRadius = config.getDouble("search_radius", 20.0)
        val maxDistance = config.getDouble("max_distance", 12.0)
        val maxDistanceSq = maxDistance * maxDistance
        val healAmount = config.getDouble("heal_amount", 1.0)

        val playerLoc = player.location

        player.getNearbyEntities(searchRadius, searchRadius, searchRadius)
            .filterIsInstance<EnderCrystal>()
            .forEach { crystal ->
                val distSq = crystal.location.distanceSquared(playerLoc)
                if (distSq <= maxDistanceSq) {
                    crystal.beamTarget = playerLoc.clone().apply { y -= 1.0 }
                    val maxHealth = player.maxHealth
                    player.health = min(maxHealth, player.health + healAmount)
                } else if (crystal.beamTarget != null) {
                    crystal.beamTarget = null
                }
            }

        true
    }
}

val oceanWish = ability("ocean_wish", "fantasyorigins") {
    title = text("Ocean Wish")
    description(
        "Your natural habitat is the ocean, so you're much weaker",
        "when you're not in the water."
    )

    option("land_damage_multiplier", -0.4)
    option("land_health_penalty", -12.0)
    option("land_speed_multiplier", -0.4)

    conditionalAttributeWhen(
        type = AttributeType.ATTACK_DAMAGE,
        value = -0.4,
        operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        checkInterval = 20
    ) { player, _ ->
        !player.isInWaterOrRainOrBubbleColumn
    }

    conditionalAttributeWhen(
        type = AttributeType.MAX_HEALTH,
        value = -12.0,
        operation = AttributeModifier.Operation.ADD_NUMBER,
        checkInterval = 20
    ) { player, _ ->
        !player.isInWaterOrRainOrBubbleColumn
    }

    conditionalAttributeWhen(
        type = AttributeType.MOVEMENT_SPEED,
        value = -0.4,
        operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        checkInterval = 20
    ) { player, _ ->
        !player.isInWaterOrRainOrBubbleColumn
    }
}

val oceansGrace = ability("oceans_grace", "fantasyorigins") {
    title = text("Ocean's Grace")
    description(
        "You are a part of the water, so you have extra health",
        "and deal extra damage when in water or rain."
    )

    option("water_damage_multiplier", 1.4)
    option("water_health_bonus", 4.0)

    conditionalAttributeWhen(
        type = AttributeType.ATTACK_DAMAGE,
        value = 1.4,
        operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        checkInterval = 20
    ) { player, _ ->
        player.isInWaterOrRainOrBubbleColumn
    }

    conditionalAttributeWhen(
        type = AttributeType.MAX_HEALTH,
        value = 4.0,
        operation = AttributeModifier.Operation.ADD_NUMBER,
        checkInterval = 20
    ) { player, _ ->
        player.isInWaterOrRainOrBubbleColumn
    }
}

val moonStrength = ability("moon_strength", "fantasyorigins") {
    title = text("Moon's Blessing")
    description(
        "You're a worshipper of the moon, so on nights with a full moon",
        "you're stronger than normal."
    )

    option("damage_multiplier", 2.4)

    conditionalAttributeWhen(
        type = AttributeType.ATTACK_DAMAGE,
        value = 2.4,
        operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        checkInterval = 40
    ) { player, _ ->
        !player.world.isDayTime && player.world.moonPhase == MoonPhase.FULL_MOON
    }
}

val environmentAbilities = listOf(
    endBoost,
    endCrystalHealing,
    oceanWish,
    oceansGrace,
    moonStrength
)
