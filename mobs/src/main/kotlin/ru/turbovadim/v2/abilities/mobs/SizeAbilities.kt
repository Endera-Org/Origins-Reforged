package ru.turbovadim.v2.abilities.mobs

import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Size-related abilities for the Mobs module.
 * These abilities typically modify health and other attributes based on size.
 *
 * Note: These abilities are attribute modifiers. The v2 system should handle
 * attribute registration via an AttributeProcessor that reads these config values.
 * The actual attribute modification is declared via options and applied by the processor.
 */

/**
 * Small Bug - reduced health for bug-like origins.
 * Legacy: AttributeModifierAbility with MAX_HEALTH -4.0 ADD_NUMBER
 */
val smallBug = ability("small_bug", "moborigins") {
    title = text("Small Bug")
    description("You have 2 less hearts of health than humans.")

    // Attribute modifier configuration
    // Processor reads these to apply: MAX_HEALTH, -4.0, ADD_NUMBER
    option("attribute", "MAX_HEALTH")
    option("health_reduction", -4.0)
    option("operation", "ADD_NUMBER")
}

/**
 * Small Fox - reduced health for fox-like origins.
 * Legacy: AttributeModifierAbility with MAX_HEALTH -4.0 ADD_NUMBER
 */
val smallFox = ability("small_fox", "moborigins") {
    title = text("Small Fox")
    description("You have 2 less hearts of health than humans.")

    option("attribute", "MAX_HEALTH")
    option("health_reduction", -4.0)
    option("operation", "ADD_NUMBER")
}

/**
 * Small Weak - reduced damage when at low health, but stronger knockback.
 * Legacy: Conditional AttributeModifierAbility - checks if health <= 4:
 *   - If true: ATTACK_DAMAGE -0.95 MULTIPLY_SCALAR_1 (95% damage reduction)
 *   - If false: no modifier
 */
val smallWeak = ability("small_weak", "moborigins") {
    title = text("Small Weakness")
    description("When at less than 2 hearts, you deal almost no damage, but your attacks have stronger knockback!")

    option("health_threshold", 4.0)
    option("damage_reduction", -0.95)
    option("attribute", "ATTACK_DAMAGE")
    option("operation", "MULTIPLY_SCALAR_1")

    // Tick handler returns the condition state for attribute processor
    // When player health <= threshold, the attribute modifier should be active
    onTick(interval = 20) { player, config ->
        val threshold = config.getDouble("health_threshold", 4.0)
        // Return true when condition is met (low health = weakness active)
        player.health <= threshold
    }
}

/**
 * Small Weak Knockback - increased knockback when at low health.
 * Hidden ability that works with SmallWeak.
 * Legacy: Conditional AttributeModifierAbility - checks if health <= 4:
 *   - If true: ATTACK_KNOCKBACK +2.5 ADD_NUMBER
 *   - If false: no modifier
 */
val smallWeakKnockback = ability("small_weak_knockback", "moborigins") {
    title = text("Small Weak Knockback")
    description("Increased knockback when at low health.")
    visible = false

    option("health_threshold", 4.0)
    option("knockback_bonus", 2.5)
    option("attribute", "ATTACK_KNOCKBACK")
    option("operation", "ADD_NUMBER")

    // Same condition as SmallWeak - active when health is low
    onTick(interval = 20) { player, config ->
        val threshold = config.getDouble("health_threshold", 4.0)
        player.health <= threshold
    }
}

/**
 * Collection of all size-related abilities.
 */
val sizeAbilities = listOf(
    smallBug,
    smallFox,
    smallWeak,
    smallWeakKnockback
)
