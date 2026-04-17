package ru.turbovadim.v2.abilities.fantasy

import org.bukkit.attribute.AttributeModifier
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Body-related abilities for the Fantasy Origins module.
 */

val doubleHealthFantasy = ability("double_health", "fantasyorigins") {
    title = text("Double Health")
    description(
        "As you're larger than humans, you have more health as your body protects you from damage."
    )

    attribute(AttributeType.MAX_HEALTH, 20.0, configKey = "health_bonus")
}

val strongSkin = ability("strong_skin", "fantasyorigins") {
    title = text("Strong Skin")
    description(
        "Your skin is much stronger than that of a regular human, providing natural protection."
    )

    attribute(AttributeType.ARMOR_TOUGHNESS, 2.0, configKey = "armor_toughness")
}

val largeBody = ability("large_body", "fantasyorigins") {
    title = text("Large Body")
    description("You are built to be much larger than other people.")

    attribute(
        AttributeType.SCALE,
        0.5,
        operation = AttributeModifier.Operation.ADD_SCALAR,
        configKey = "scale_modifier"
    )
}

val smallBody = ability("small_body", "fantasyorigins") {
    title = text("Small Body")
    description("You are built to be much smaller than other people.")

    attribute(
        AttributeType.SCALE,
        -0.5,
        operation = AttributeModifier.Operation.ADD_SCALAR,
        configKey = "scale_modifier"
    )
}

val stronger = ability("stronger", "fantasyorigins") {
    title = text("Stronger")
    description(
        "Your vampiric nature makes you stronger than a regular human,",
        "making your physical attacks deal far more damage."
    )

    attribute(
        AttributeType.ATTACK_DAMAGE,
        1.8,
        operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        configKey = "damage_multiplier"
    )
}

val bodyAbilities = listOf(
    doubleHealthFantasy,
    strongSkin,
    largeBody,
    smallBody,
    stronger
)
