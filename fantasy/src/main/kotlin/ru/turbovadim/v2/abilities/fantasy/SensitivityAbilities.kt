package ru.turbovadim.v2.abilities.fantasy

import org.bukkit.World
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Sensitivity and weakness abilities for the Fantasy Origins module.
 */

val daylightSensitive = ability("daylight_sensitive", "fantasyorigins") {
    title = text("Daylight Sensitivity")
    description("Your dark nature makes you burn when exposed to daylight.")

    option("fire_ticks", 60)

    onTick(interval = 15) { player, config ->
        val world = player.world
        val exposed = world.environment == World.Environment.NORMAL &&
            world.isDayTime &&
            !player.isInWaterOrRainOrBubbleColumn &&
            player.eyeLocation.block.lightFromSky >= 15

        if (exposed) {
            player.fireTicks = player.fireTicks.coerceAtLeast(config.getInt("fire_ticks", 60))
        }
        exposed
    }
}

val waterSensitive = ability("water_sensitive", "fantasyorigins") {
    title = text("Water Sensitive")
    description("Your dark nature makes it so that water damages you.")

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
