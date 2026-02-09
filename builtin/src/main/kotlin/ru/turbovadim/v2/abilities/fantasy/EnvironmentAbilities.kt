package ru.turbovadim.v2.abilities.fantasy

import io.papermc.paper.world.MoonPhase
import org.bukkit.World
import org.bukkit.entity.EnderCrystal
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text
import kotlin.math.min

/**
 * Environment and dimension-related abilities for the Fantasy Origins module.
 * These abilities provide bonuses or effects based on the player's environment.
 *
 * Most of these use conditional attribute modifiers that are applied/removed
 * based on periodic environment checks.
 */

/**
 * End Inhabitant - Stronger and healthier in the End dimension.
 * Legacy: EndBoost (multi-ability containing EndStrength and EndHealth)
 *
 * Implementation: Conditional attribute modifiers that apply when the player
 * is in THE_END dimension. Checked periodically.
 *
 * Config attributes (conditional - in the End):
 *   - attribute: generic-attack-damage
 *     value: 1.6
 *     operation: multiply-scalar-1
 *   - attribute: generic-max-health
 *     value: 20.0
 *     operation: add-number
 */
val endBoost = ability("end_boost", "fantasyorigins") {
    title = text("End Inhabitant")
    description(
        "Your natural habitat is the end, so you have more health",
        "and are stronger when you are there."
    )

    option("damage_multiplier", 1.6)
    option("health_bonus", 20.0)

    // Check if player is in the End dimension
    // The attribute modifier system uses this result to apply/remove modifiers
    onTick(interval = 20) { player, config ->
        player.world.environment == World.Environment.THE_END
    }
}

/**
 * Crystal Healer - Regenerate health from nearby End Crystals.
 * Legacy: EndCrystalHealing
 *
 * Implementation: Periodically scans for End Crystals within range.
 * If found, heals the player and points the crystal's beam at them.
 */
val endCrystalHealing = ability("end_crystal_healing", "fantasyorigins") {
    title = text("Crystal Healer")
    description("You can regenerate health from nearby End Crystals.")

    option("search_radius", 20.0)
    option("max_distance", 12.0)
    option("heal_amount", 1.0)
    option("interval", 5)

    onTick(interval = 5) { player, config ->
        val searchRadius = config.getDouble("search_radius", 20.0)
        val maxDistanceSq = config.getDouble("max_distance", 12.0).let { it * it }
        val healAmount = config.getDouble("heal_amount", 1.0)

        val playerLoc = player.location

        // Find nearby End Crystals
        player.getNearbyEntities(searchRadius, searchRadius, searchRadius)
            .filterIsInstance<EnderCrystal>()
            .filter { it.location.distanceSquared(playerLoc) <= maxDistanceSq }
            .forEach { crystal ->
                // Point the crystal's beam at the player
                crystal.beamTarget = playerLoc.clone().apply { y -= 1.0 }

                // Heal the player
                val maxHealth = player.maxHealth
                player.health = min(maxHealth, player.health + healAmount)
            }

        // Also clear beams from crystals that are too far
        player.getNearbyEntities(searchRadius, searchRadius, searchRadius)
            .filterIsInstance<EnderCrystal>()
            .filter { it.location.distanceSquared(playerLoc) > maxDistanceSq }
            .forEach { crystal ->
                if (crystal.beamTarget != null) {
                    crystal.beamTarget = null
                }
            }

        true // Always return true for the check
    }
}

/**
 * Ocean Wish - Weaker on land, normal in water.
 * Legacy: OceanWish (multi-ability containing LandSlowness, LandHealth, LandWeakness)
 *
 * Implementation: Conditional attribute modifiers that apply negative effects
 * when the player is NOT in water/rain/bubble column.
 *
 * Config attributes (conditional - not in water):
 *   - attribute: generic-attack-damage
 *     value: -0.4
 *     operation: multiply-scalar-1
 *   - attribute: generic-max-health
 *     value: -12.0
 *     operation: add-number
 *   - attribute: generic-movement-speed
 *     value: -0.4
 *     operation: multiply-scalar-1
 */
val oceanWish = ability("ocean_wish", "fantasyorigins") {
    title = text("Ocean Wish")
    description(
        "Your natural habitat is the ocean, so you're much weaker",
        "when you're not in the water."
    )

    option("land_damage_multiplier", -0.4)
    option("land_health_penalty", -12.0)
    option("land_speed_multiplier", -0.4)

    // Check if player is NOT in water - penalties apply when on land
    // Returns true when player should have penalties (NOT in water)
    onTick(interval = 20) { player, config ->
        !player.isInWaterOrRainOrBubbleColumn
    }
}

/**
 * Ocean's Grace - Stronger and healthier in water or rain.
 * Legacy: OceansGrace (multi-ability containing WaterStrength, WaterHealth)
 *
 * Implementation: Conditional attribute modifiers that apply bonuses
 * when the player IS in water/rain/bubble column.
 *
 * Config attributes (conditional - in water/rain):
 *   - attribute: generic-attack-damage
 *     value: 1.4
 *     operation: multiply-scalar-1
 *   - attribute: generic-max-health
 *     value: 4.0
 *     operation: add-number
 */
val oceansGrace = ability("oceans_grace", "fantasyorigins") {
    title = text("Ocean's Grace")
    description(
        "You are a part of the water, so you have extra health",
        "and deal extra damage when in water or rain."
    )

    option("water_damage_multiplier", 1.4)
    option("water_health_bonus", 4.0)

    // Check if player is in water - bonuses apply when in water
    onTick(interval = 20) { player, config ->
        player.isInWaterOrRainOrBubbleColumn
    }
}

/**
 * Moon's Blessing - Stronger during full moon nights.
 * Legacy: MoonStrength
 *
 * Implementation: Conditional attribute modifier that applies extra attack damage
 * when it's nighttime AND the moon phase is FULL_MOON.
 *
 * Config attributes (conditional - full moon at night):
 *   - attribute: generic-attack-damage
 *     value: 2.4
 *     operation: multiply-scalar-1
 */
val moonStrength = ability("moon_strength", "fantasyorigins") {
    title = text("Moon's Blessing")
    description(
        "You're a worshipper of the moon, so on nights with a full moon",
        "you're stronger than normal."
    )

    option("damage_multiplier", 2.4)

    // Check if it's nighttime with a full moon
    onTick(interval = 20) { player, config ->
        !player.world.isDayTime && player.world.moonPhase == MoonPhase.FULL_MOON
    }
}

/**
 * Collection of all environment-related abilities.
 */
val environmentAbilities = listOf(
    endBoost,
    endCrystalHealing,
    oceanWish,
    oceansGrace,
    moonStrength
)
