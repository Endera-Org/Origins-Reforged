package ru.turbovadim.v2.abilities.monsters

import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Potion effect abilities for monster origins.
 * Includes passive effects, immunities, and effect applications.
 */

// ============================================
// VISION ABILITIES
// ============================================

/**
 * Night vision when on land (for drowned-type origins).
 * Grants infinite night vision when not underwater, restores previous effect when entering water.
 */
val landNightVision = ability("land_night_vision", "monsterorigins") {
    title = text("Dark Sight")
    description("You can see in the dark when on land.")

    option("check_interval", 1)
    option("effect_duration", 400)

    // Night vision when not underwater
    // Note: Full implementation with effect storage/restoration requires state management
    // Legacy behavior: stores existing night vision, applies infinite amplifier -1 effect,
    // restores original when entering water
    onTick(interval = 1) { player, config ->
        if (!player.isUnderWater) {
            val currentEffect = player.getPotionEffect(PotionEffectType.NIGHT_VISION)
            val ambient = currentEffect?.isAmbient ?: false
            val showParticles = currentEffect?.hasParticles() ?: false

            // Apply infinite night vision with amplifier -1 (special marker)
            player.addPotionEffect(
                PotionEffect(
                    PotionEffectType.NIGHT_VISION,
                    Int.MAX_VALUE, // Infinite duration
                    -1, // Special amplifier to identify ability-granted effect
                    ambient,
                    showParticles
                )
            )
        } else {
            // Remove ability-granted night vision when underwater
            val effect = player.getPotionEffect(PotionEffectType.NIGHT_VISION)
            if (effect != null && effect.amplifier == -1) {
                player.removePotionEffect(PotionEffectType.NIGHT_VISION)
            }
        }
        true
    }
}

/**
 * Blindness effect - uses darkness when player has night vision, blindness otherwise.
 */
val blindness = ability("blindness", "monsterorigins") {
    title = text("Blindness")
    description(
        "You can't see anything further than a few blocks away,",
        "though you can see further with night vision."
    )

    option("effect_duration", 240)
    option("check_interval", 5)

    onTick(interval = 5) { player, config ->
        val duration = config.getInt("effect_duration", 240)

        if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            // Has night vision: use darkness instead of blindness
            player.removePotionEffect(PotionEffectType.BLINDNESS)
            player.addPotionEffect(
                PotionEffect(PotionEffectType.DARKNESS, duration, 0, false, false)
            )
        } else {
            // No night vision: use blindness
            player.removePotionEffect(PotionEffectType.DARKNESS)
            player.addPotionEffect(
                PotionEffect(PotionEffectType.BLINDNESS, duration, 0, false, false)
            )
        }
        true
    }
}

// ============================================
// POTION IMMUNITIES
// ============================================

/**
 * Immunity to Wither effect.
 * Note: This requires event-based handling (EntityPotionEffectEvent) to cancel wither application.
 * The v2 DSL doesn't have a direct potion immunity handler yet.
 */
val witherImmunity = ability("wither_immunity", "monsterorigins") {
    title = text("Wither Immunity")
    description("You are immune to the Wither effect.")
    visible = false

    // Note: Full implementation requires EntityPotionEffectEvent handling
    // to cancel wither effect application. Legacy behavior:
    // if (event.newEffect?.type == PotionEffectType.WITHER) event.isCancelled = true
}

/**
 * Immunity to freeze damage.
 * Periodically resets freeze ticks to 0.
 */
val freezeImmune = ability("freeze_immune", "monsterorigins") {
    title = text("Freeze Immunity")
    description("You are immune to freezing.")
    visible = false

    onTick(interval = 1) { player, _ ->
        player.freezeTicks = 0
        true
    }
}

// ============================================
// FEAR/DEBUFF EFFECTS
// ============================================

/**
 * Fear of cats - causes nausea and weakness near cats.
 */
val fearCats = ability("fear_cats", "monsterorigins") {
    title = text("Afraid of Cats")
    description("You get nausea and weakness when around cats.")

    option("detection_radius", 8.0)
    option("effect_duration", 200)
    option("check_interval", 5)

    onTick(interval = 5) { player, config ->
        val radius = config.getDouble("detection_radius", 8.0)
        val duration = config.getInt("effect_duration", 200)

        val catsNearby = player.getNearbyEntities(radius, radius, radius)
            .any { it.type == EntityType.CAT }

        if (catsNearby) {
            // Apply nausea (CONFUSION in older API versions)
            @Suppress("DEPRECATION")
            val nauseaEffect = try {
                PotionEffectType.getByName("NAUSEA") ?: PotionEffectType.CONFUSION
            } catch (_: Exception) {
                PotionEffectType.CONFUSION
            }
            player.addPotionEffect(
                PotionEffect(nauseaEffect, duration, 0, false, true)
            )
            player.addPotionEffect(
                PotionEffect(PotionEffectType.WEAKNESS, duration, 0, false, true)
            )
        }
        true
    }
}

// ============================================
// ON-HIT EFFECTS
// ============================================

/**
 * Apply Wither effect on hit.
 */
val applyWitherEffect = ability("apply_wither_effect", "monsterorigins") {
    title = text("Wither")
    description("Anything you hit gets the Wither effect.")

    option("effect_duration", 200)
    option("effect_amplifier", 0)

    onAttack { _, target, config ->
        if (target is LivingEntity) {
            val duration = config.getInt("effect_duration", 200)
            val amplifier = config.getInt("effect_amplifier", 0)
            target.addPotionEffect(
                PotionEffect(PotionEffectType.WITHER, duration, amplifier, false, true)
            )
        }
    }
}

/**
 * Apply Hunger effect on hit.
 */
val applyHungerEffect = ability("apply_hunger_effect", "monsterorigins") {
    title = text("Hunger")
    description("Anything you hit gets the Hunger effect.")

    option("effect_duration", 200)
    option("effect_amplifier", 0)

    onAttack { _, target, config ->
        if (target is LivingEntity) {
            val duration = config.getInt("effect_duration", 200)
            val amplifier = config.getInt("effect_amplifier", 0)
            target.addPotionEffect(
                PotionEffect(PotionEffectType.HUNGER, duration, amplifier, false, true)
            )
        }
    }
}
