package ru.turbovadim.v2.abilities.fantasy

import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Body-related abilities for the Fantasy Origins module.
 * These abilities modify health, size, armor, and general physical attributes.
 *
 * Note: These abilities use attribute modifiers which are applied via the config system.
 * The DSL definitions provide metadata, default option values, and serve as markers
 * for the attribute modifier system to apply the correct modifiers.
 *
 * Attribute modifiers are applied when the player's origin is set/changed.
 * The actual modifier values are read from abilities.yml config.
 */

/**
 * Double Health - More health due to larger body size.
 * Legacy: DoubleHealthFantasy
 *
 * Implementation: Applies an attribute modifier to generic.max_health.
 * The modifier adds 20.0 health (10 hearts) to the player's maximum health.
 *
 * Config attributes:
 *   - attribute: generic-max-health
 *     value: 20.0
 *     operation: add-number
 */
val doubleHealthFantasy = ability("double_health", "fantasyorigins") {
    title = text("Double Health")
    description(
        "As you're larger than humans, you have more health as your body protects you from damage."
    )

    // This option defines the default health bonus for config generation
    option("health_bonus", 20.0)

    // Note: The actual attribute modifier is applied by the attribute modifier system
    // which reads from abilities.yml. This ability acts as a marker.
}

/**
 * Strong Skin - Natural armor toughness.
 * Legacy: StrongSkin
 *
 * Implementation: Applies an attribute modifier to generic.armor_toughness.
 * Armor toughness reduces the effectiveness of high-damage attacks against armor.
 *
 * Config attributes:
 *   - attribute: generic-armor-toughness
 *     value: 2.0
 *     operation: add-number
 */
val strongSkin = ability("strong_skin", "fantasyorigins") {
    title = text("Strong Skin")
    description(
        "Your skin is much stronger than that of a regular human, providing natural protection."
    )

    option("armor_toughness", 2.0)

    // Note: Attribute modifier applied via config system
}

/**
 * Large Body - Increased player scale.
 * Legacy: LargeBody
 *
 * Implementation: Applies an attribute modifier to generic.scale (1.20.5+).
 * Makes the player model 50% larger than default.
 *
 * Config attributes:
 *   - attribute: generic-scale
 *     value: 0.5
 *     operation: add-scalar
 */
val largeBody = ability("large_body", "fantasyorigins") {
    title = text("Large Body")
    description("You are built to be much larger than other people.")

    option("scale_modifier", 0.5)

    // Note: Scale attribute only available in 1.20.5+
    // Attribute modifier applied via config system
}

/**
 * Small Body - Decreased player scale.
 * Legacy: SmallBody
 *
 * Implementation: Applies an attribute modifier to generic.scale (1.20.5+).
 * Makes the player model 50% smaller than default.
 *
 * Config attributes:
 *   - attribute: generic-scale
 *     value: -0.5
 *     operation: add-scalar
 */
val smallBody = ability("small_body", "fantasyorigins") {
    title = text("Small Body")
    description("You are built to be much smaller than other people.")

    option("scale_modifier", -0.5)

    // Note: Scale attribute only available in 1.20.5+
    // Attribute modifier applied via config system
}

/**
 * Stronger - Increased attack damage (vampiric strength).
 * Legacy: Stronger
 *
 * Implementation: Applies an attribute modifier to generic.attack_damage.
 * Uses MULTIPLY_SCALAR_1 operation, which multiplies the final damage by (1 + value).
 * With value 1.8, damage is multiplied by 2.8x.
 *
 * Config attributes:
 *   - attribute: generic-attack-damage
 *     value: 1.8
 *     operation: multiply-scalar-1
 */
val stronger = ability("stronger", "fantasyorigins") {
    title = text("Stronger")
    description(
        "Your vampiric nature makes you stronger than a regular human,",
        "making your physical attacks deal far more damage."
    )

    option("damage_multiplier", 1.8)

    // Note: Attribute modifier applied via config system
}

/**
 * Collection of all body-related abilities.
 */
val bodyAbilities = listOf(
    doubleHealthFantasy,
    strongSkin,
    largeBody,
    smallBody,
    stronger
)
