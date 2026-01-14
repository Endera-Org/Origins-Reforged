package ru.turbovadim.v2.abilities.fantasy

import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Potion effect abilities for the Fantasy Origins module.
 * These abilities grant permanent or periodic potion effects.
 *
 * Potion effects are applied using the DSL's applyPotion() which creates a
 * periodic effect that reapplies the potion at regular intervals to maintain it.
 */

/**
 * Night Vision - Permanent night vision effect.
 * Legacy: InfiniteNightVision
 *
 * Implementation: Applies night vision every 15 ticks with 240 tick duration.
 * This ensures the effect never expires while the player has this ability.
 */
val infiniteNightVision = ability("infinite_night_vision", "fantasyorigins") {
    title = text("Night Vision")
    description("Your eyes are adapted to see clearly in the dark.")

    option("interval", 15)
    option("duration", 240)

    applyPotion(
        type = PotionEffectType.NIGHT_VISION,
        duration = 240,
        amplifier = 0,
        interval = 15
    )
}

/**
 * Fast Miner - Permanent haste effect for faster mining.
 * Legacy: InfiniteHaste
 *
 * Implementation: Applies haste (fast digging) every 20 ticks with 30 tick duration.
 * Amplifier 1 gives Haste II effect.
 *
 * Note: Uses PotionEffectType.FAST_DIGGING (not HASTE) for Paper 1.20.1 compatibility.
 */
val infiniteHaste = ability("infinite_haste", "fantasyorigins") {
    title = text("Fast Miner")
    description("You're well trained in mining, so are much faster than a regular human.")

    option("interval", 20)
    option("duration", 30)
    option("amplifier", 1)

    applyPotion(
        type = PotionEffectType.FAST_DIGGING,
        duration = 30,
        amplifier = 1,
        interval = 20
    )
}

/**
 * Dashmaster - Indicates increased movement speed.
 * Legacy: IncreasedSpeed
 *
 * Implementation: This ability is a marker for the centaur origin's horse mount.
 * The actual speed boost is applied to the horse entity via attribute modifiers
 * in the PermanentHorse ability. This ability serves as UI display only.
 *
 * Config attributes (applied to horse, not player):
 *   - attribute: generic-movement-speed
 *     value: 0.4
 *     operation: add-number
 */
val increasedSpeed = ability("increased_speed", "fantasyorigins") {
    title = text("Dashmaster")
    description(
        "From years of training for race after race,",
        "you're much faster than any normal horse."
    )

    option("speed_bonus", 0.4)

    // Note: This is a marker ability. The actual speed boost is applied to the
    // centaur's horse mount in the PermanentHorse ability when it detects this ability.
}

/**
 * Bouncing - Indicates enhanced jump height.
 * Legacy: SuperJump
 *
 * Implementation: This ability is a marker for the centaur origin's horse mount.
 * The actual jump boost is applied to the horse entity via attribute modifiers
 * in the PermanentHorse ability. This ability serves as UI display only.
 *
 * Config attributes (applied to horse, not player):
 *   - attribute: generic-jump-strength
 *     value: 1.0
 *     operation: add-number
 */
val superJump = ability("super_jump", "fantasyorigins") {
    title = text("Bouncing")
    description(
        "For years you've always felt as if your legs don't do quite enough,",
        "with all this training you can reach even higher heights."
    )

    option("jump_strength", 1.0)

    // Note: This is a marker ability. The actual jump boost is applied to the
    // centaur's horse mount in the PermanentHorse ability when it detects this ability.
}

/**
 * Collection of all potion effect abilities.
 */
val potionEffectAbilities = listOf(
    infiniteNightVision,
    infiniteHaste,
    increasedSpeed,
    superJump
)
