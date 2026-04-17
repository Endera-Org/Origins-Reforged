package ru.turbovadim.v2.abilities.mobs

import org.bukkit.attribute.AttributeModifier
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Size-related abilities for the Mobs module.
 */

val smallBug = ability("small_bug", "moborigins") {
    title = text("Small Bug")
    description("You have 2 less hearts of health than humans.")

    attribute(AttributeType.MAX_HEALTH, -4.0, configKey = "health_reduction")
}

val smallFox = ability("small_fox", "moborigins") {
    title = text("Small Fox")
    description("You have 2 less hearts of health than humans.")

    attribute(AttributeType.MAX_HEALTH, -4.0, configKey = "health_reduction")
}

/**
 * Small Weak - reduced damage when at low health.
 */
val smallWeak = ability("small_weak", "moborigins") {
    title = text("Small Weakness")
    description("When at less than 2 hearts, you deal almost no damage, but your attacks have stronger knockback!")

    option("health_threshold", 4.0)

    conditionalAttributeWhen(
        type = AttributeType.ATTACK_DAMAGE,
        value = -0.95,
        operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        checkInterval = 20,
        condition = { player, config ->
            player.health <= config.getDouble("health_threshold", 4.0)
        }
    )
}

/**
 * Small Weak Knockback - increased knockback when at low health.
 */
val smallWeakKnockback = ability("small_weak_knockback", "moborigins") {
    title = text("Small Weak Knockback")
    description("Increased knockback when at low health.")
    visible = false

    option("health_threshold", 4.0)

    conditionalAttributeWhen(
        type = AttributeType.ATTACK_KNOCKBACK,
        value = 2.5,
        operation = AttributeModifier.Operation.ADD_NUMBER,
        checkInterval = 20,
        condition = { player, config ->
            player.health <= config.getDouble("health_threshold", 4.0)
        }
    )
}

val sizeAbilities = listOf(
    smallBug,
    smallFox,
    smallWeak,
    smallWeakKnockback
)
