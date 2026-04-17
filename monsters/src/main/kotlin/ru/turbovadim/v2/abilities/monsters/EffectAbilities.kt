package ru.turbovadim.v2.abilities.monsters

import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text

/**
 * Potion effect abilities for monster origins.
 */

val landNightVision = ability("land_night_vision", "monsterorigins") {
    title = text("Dark Sight")
    description("You can see in the dark when on land.")

    onTick(interval = 1) { player, _ ->
        if (!player.isUnderWater) {
            val currentEffect = player.getPotionEffect(PotionEffectType.NIGHT_VISION)
            val ambient = currentEffect?.isAmbient ?: false
            val showParticles = currentEffect?.hasParticles() ?: false
            player.addPotionEffect(
                PotionEffect(
                    PotionEffectType.NIGHT_VISION,
                    Int.MAX_VALUE,
                    -1,
                    ambient,
                    showParticles
                )
            )
        } else {
            val effect = player.getPotionEffect(PotionEffectType.NIGHT_VISION)
            if (effect != null && effect.amplifier == -1) {
                player.removePotionEffect(PotionEffectType.NIGHT_VISION)
            }
        }
        true
    }
}

val blindness = ability("blindness", "monsterorigins") {
    title = text("Blindness")
    description(
        "You can't see anything further than a few blocks away,",
        "though you can see further with night vision."
    )

    option("effect_duration", 240)

    onTick(interval = 5) { player, config ->
        val duration = config.getInt("effect_duration", 240)

        if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            player.removePotionEffect(PotionEffectType.BLINDNESS)
            player.addPotionEffect(
                PotionEffect(PotionEffectType.DARKNESS, duration, 0, false, false)
            )
        } else {
            player.removePotionEffect(PotionEffectType.DARKNESS)
            player.addPotionEffect(
                PotionEffect(PotionEffectType.BLINDNESS, duration, 0, false, false)
            )
        }
        true
    }
}

val witherImmunity = ability("wither_immunity", "monsterorigins") {
    title = text("Wither Immunity")
    description("You are immune to the Wither effect.")
    visible = false

    listener<EntityPotionEffectEvent>(
        ignoreCancelled = false,
        playerFrom = { it.entity as? Player }
    ) { _, event, _ ->
        if (event.newEffect?.type == PotionEffectType.WITHER) {
            event.isCancelled = true
        }
    }
}

val freezeImmune = ability("freeze_immune", "monsterorigins") {
    title = text("Freeze Immunity")
    description("You are immune to freezing.")
    visible = false

    onTick(interval = 1) { player, _ ->
        player.freezeTicks = 0
        true
    }
}

val fearCats = ability("fear_cats", "monsterorigins") {
    title = text("Afraid of Cats")
    description("You get nausea and weakness when around cats.")

    option("detection_radius", 8.0)
    option("effect_duration", 200)

    onTick(interval = 5) { player, config ->
        val radius = config.getDouble("detection_radius", 8.0)
        val duration = config.getInt("effect_duration", 200)

        val catsNearby = player.getNearbyEntities(radius, radius, radius)
            .any { it.type == EntityType.CAT }

        if (catsNearby) {
            player.addPotionEffect(
                PotionEffect(OriginsReforged.NMSInvoker.nauseaEffect, duration, 0, false, true)
            )
            player.addPotionEffect(
                PotionEffect(PotionEffectType.WEAKNESS, duration, 0, false, true)
            )
        }
        true
    }
}

val applyWitherEffect = ability("apply_wither_effect", "monsterorigins") {
    title = text("Wither")
    description("Anything you hit gets the Wither effect.")

    option("effect_duration", 200)
    option("effect_amplifier", 0)

    onAttack { _, target, config ->
        if (target !is LivingEntity) return@onAttack
        val duration = config.getInt("effect_duration", 200)
        val amplifier = config.getInt("effect_amplifier", 0)
        target.addPotionEffect(
            PotionEffect(PotionEffectType.WITHER, duration, amplifier, false, true)
        )
    }
}

val applyHungerEffect = ability("apply_hunger_effect", "monsterorigins") {
    title = text("Hunger")
    description("Anything you hit gets the Hunger effect.")

    option("effect_duration", 200)
    option("effect_amplifier", 0)

    onAttack { _, target, config ->
        if (target !is LivingEntity) return@onAttack
        val duration = config.getInt("effect_duration", 200)
        val amplifier = config.getInt("effect_amplifier", 0)
        target.addPotionEffect(
            PotionEffect(PotionEffectType.HUNGER, duration, amplifier, false, true)
        )
    }
}
