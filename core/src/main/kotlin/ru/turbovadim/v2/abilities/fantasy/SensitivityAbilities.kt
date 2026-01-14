package ru.turbovadim.v2.abilities.fantasy

import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Sensitivity and weakness abilities for the Fantasy Origins module.
 * These abilities cause negative effects from environmental factors.
 *
 * These are primarily marker abilities that indicate sensitivities.
 * The actual damage/weakness effects are typically handled by:
 * 1. The burning system (for daylight sensitivity with vampires)
 * 2. Periodic damage checks (for water sensitivity)
 * 3. Conditional attribute modifiers (for stat reductions)
 */

/**
 * Daylight Sensitivity - Weaker during daylight hours.
 * Legacy: DaylightSensitive
 *
 * Implementation: This is a marker ability that other systems check for.
 * The actual effects (burning, weakness) are typically applied by:
 * - The vampire burning system (separate ability)
 * - Conditional attribute modifiers that reduce stats during day
 *
 * The periodic check returns true when the player is exposed to daylight,
 * which triggers the conditional attribute modifiers.
 */
val daylightSensitive = ability("daylight_sensitive", "fantasyorigins") {
    title = text("Daylight Sensitivity")
    description("Your dark nature makes it so that you become less powerful during daylight.")

    // Check if player is exposed to sunlight
    // This enables conditional attribute modifiers during daylight
    onTick(interval = 20) { player, config ->
        val world = player.world
        // Check if it's daytime and player can see the sky
        val isDayTime = world.isDayTime
        val canSeeSky = player.location.block.lightFromSky >= 15

        // Player is affected by daylight if both conditions are met
        isDayTime && canSeeSky && !player.isInWaterOrRainOrBubbleColumn
    }
}

/**
 * Water Sensitive - Damaged by water contact.
 * Legacy: WaterSensitive
 *
 * Implementation: This ability causes the player to take damage when in contact
 * with water (in water, rain, or bubble column). Damage is applied periodically.
 *
 * Note: This is a marker ability. The actual damage application is handled
 * by a separate system that checks for this ability and applies damage
 * periodically when the player is in water.
 */
val waterSensitive = ability("water_sensitive", "fantasyorigins") {
    title = text("Water Sensitive")
    description("Your fiery nature makes it so that water damages you.")

    option("water_damage", 1.0)
    option("damage_interval", 20)

    // Check if player is in water and apply damage
    onTick(interval = 20) { player, config ->
        if (player.isInWaterOrRainOrBubbleColumn) {
            val damage = config.getDouble("water_damage", 1.0)
            // Apply damage using the DROWNING cause (closest to water damage)
            player.damage(damage)
        }
        // Return whether player is in water (for conditional effects)
        player.isInWaterOrRainOrBubbleColumn
    }
}

/**
 * Collection of all sensitivity-related abilities.
 */
val sensitivityAbilities = listOf(
    daylightSensitive,
    waterSensitive
)
