package ru.turbovadim.v2.abilities.fantasy

import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Permanent potion effect abilities for the Fantasy Origins module.
 */

val infiniteNightVision = ability("infinite_night_vision", "fantasyorigins") {
    title = text("Night Vision")
    description("Your eyes are adapted to see clearly in the dark.")

    applyPotion(type = PotionEffectType.NIGHT_VISION, duration = 240, interval = 15)
}

val infiniteHaste = ability("infinite_haste", "fantasyorigins") {
    title = text("Fast Miner")
    description("You're well trained in mining, so are much faster than a regular human.")

    applyPotion(type = PotionEffectType.HASTE, duration = 30, amplifier = 1, interval = 20)
}

val potionEffectAbilities = listOf(
    infiniteNightVision,
    infiniteHaste
)
