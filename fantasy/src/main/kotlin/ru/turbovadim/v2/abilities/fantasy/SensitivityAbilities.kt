package ru.turbovadim.v2.abilities.fantasy

import org.bukkit.attribute.AttributeModifier
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Sensitivity and weakness abilities for the Fantasy Origins module.
 */

val daylightSensitive = ability("daylight_sensitive", "fantasyorigins") {
    title = text("Daylight Sensitivity")
    description("Your dark nature makes it so that you become less powerful during daylight.")

    option("damage_multiplier", -0.25)
    option("health_penalty", -4.0)

    conditionalAttributeWhen(
        type = AttributeType.ATTACK_DAMAGE,
        value = -0.25,
        operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        checkInterval = 20
    ) { player, _ ->
        isExposedToDaylight(player)
    }

    conditionalAttributeWhen(
        type = AttributeType.MAX_HEALTH,
        value = -4.0,
        operation = AttributeModifier.Operation.ADD_NUMBER,
        checkInterval = 20
    ) { player, _ ->
        isExposedToDaylight(player)
    }
}

private fun isExposedToDaylight(player: org.bukkit.entity.Player): Boolean {
    val world = player.world
    return world.isDayTime &&
        player.location.block.lightFromSky >= 15 &&
        !player.isInWaterOrRainOrBubbleColumn
}

val waterSensitive = ability("water_sensitive", "fantasyorigins") {
    title = text("Water Sensitive")
    description("Your fiery nature makes it so that water damages you.")

    option("water_damage", 1.0)

    onTick(interval = 20) { player, config ->
        if (player.isInWaterOrRainOrBubbleColumn) {
            val damage = config.getDouble("water_damage", 1.0)
            player.damage(damage)
            true
        } else {
            false
        }
    }
}

val sensitivityAbilities = listOf(
    daylightSensitive,
    waterSensitive
)
