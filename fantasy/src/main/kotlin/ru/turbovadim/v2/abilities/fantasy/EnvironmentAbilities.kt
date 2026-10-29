package ru.turbovadim.v2.abilities.fantasy

import io.papermc.paper.world.MoonPhase
import org.bukkit.World
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.EnderCrystal
import org.bukkit.entity.Player
import org.endera.enderalib.utils.async.runTask
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.dsl.AbilityBuilder
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text
import kotlin.math.min

/**
 * Environment and dimension-related abilities for the Fantasy Origins module.
 */

/**
 * Applies the configurable modifier [configKey] (defaulting to [default]) while [active] holds.
 */
internal fun AbilityBuilder.attributeWhile(
    type: AttributeType,
    configKey: String,
    default: Double,
    operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER,
    checkInterval: Int = 20,
    active: (Player) -> Boolean
) {
    option(configKey, default)
    conditionalAttribute(type, operation, checkInterval) { player, config ->
        if (active(player)) config.getDouble(configKey, default) else 0.0
    }
}

private val isInEnd: (Player) -> Boolean = { it.world.environment == World.Environment.THE_END }
private val isWet: (Player) -> Boolean = { it.isInWaterOrRainOrBubbleColumn }

val endBoost = ability("end_boost", "fantasyorigins") {
    title = text("End Inhabitant")
    description(
        "Your natural habitat is the end, so you have more health",
        "and are stronger when you are there."
    )

    attributeWhile(AttributeType.ATTACK_DAMAGE, "damage_multiplier", 1.6, AttributeModifier.Operation.MULTIPLY_SCALAR_1, active = isInEnd)
    attributeWhile(AttributeType.MAX_HEALTH, "health_bonus", 20.0, active = isInEnd)
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
        val beamTargetLoc = playerLoc.clone().apply { y -= 1.0 }

        player.getNearbyEntities(searchRadius, searchRadius, searchRadius)
            .filterIsInstance<EnderCrystal>()
            .forEach { crystal ->
                val distSq = crystal.location.distanceSquared(playerLoc)
                if (distSq <= maxDistanceSq) {
                    val maxHealth = player.maxHealth
                    player.health = min(maxHealth, player.health + healAmount)

                    crystal.runTask(OriginsReforged.instance) {
                        if (crystal.isValid) {
                            crystal.beamTarget = beamTargetLoc
                        }
                    }
                } else {
                    crystal.runTask(OriginsReforged.instance) {
                        if (crystal.isValid && crystal.beamTarget != null) {
                            crystal.beamTarget = null
                        }
                    }
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

    val onLand: (Player) -> Boolean = { !isWet(it) }
    attributeWhile(AttributeType.ATTACK_DAMAGE, "land_damage_multiplier", -0.4, AttributeModifier.Operation.MULTIPLY_SCALAR_1, active = onLand)
    attributeWhile(AttributeType.MAX_HEALTH, "land_health_penalty", -12.0, active = onLand)
    attributeWhile(AttributeType.MOVEMENT_SPEED, "land_speed_multiplier", -0.4, AttributeModifier.Operation.MULTIPLY_SCALAR_1, active = onLand)
}

val oceansGrace = ability("oceans_grace", "fantasyorigins") {
    title = text("Ocean's Grace")
    description(
        "You are a part of the water, so you have extra health",
        "and deal extra damage when in water or rain."
    )

    attributeWhile(AttributeType.ATTACK_DAMAGE, "water_damage_multiplier", 1.4, AttributeModifier.Operation.MULTIPLY_SCALAR_1, active = isWet)
    attributeWhile(AttributeType.MAX_HEALTH, "water_health_bonus", 4.0, active = isWet)
}

val moonStrength = ability("moon_strength", "fantasyorigins") {
    title = text("Moon's Blessing")
    description(
        "You're a worshipper of the moon, so on nights with a full moon",
        "you're stronger than normal."
    )

    attributeWhile(
        AttributeType.ATTACK_DAMAGE,
        "damage_multiplier",
        2.4,
        AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        checkInterval = 40
    ) { player -> !player.world.isDayTime && player.world.moonPhase == MoonPhase.FULL_MOON }
}

val environmentAbilities = listOf(
    endBoost,
    endCrystalHealing,
    oceanWish,
    oceansGrace,
    moonStrength
)
