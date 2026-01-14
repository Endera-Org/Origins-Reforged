package ru.turbovadim.v2.abilities.mobs

import io.papermc.paper.world.MoonPhase
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Wolf-related abilities for the Mobs module.
 */

/**
 * Wolf Body - reduced health like a wolf.
 * Legacy: AttributeModifierAbility with MAX_HEALTH -4.0 ADD_NUMBER
 */
val wolfBody = ability("wolf_body", "moborigins") {
    title = text("Wolf Body")
    description("You have 2 less hearts of health than humans.")

    option("health_reduction", -4.0)
    option("attribute", "MAX_HEALTH")
    option("operation", "ADD_NUMBER")
}

/**
 * Alpha Wolf - wolves you tame are stronger.
 * Legacy: On EntityTameEvent and EntityBreedEvent for wolves:
 *   - Increases wolf's MAX_HEALTH base value by +10
 *   - Increases wolf's ATTACK_DAMAGE base value by +2
 * Note: Requires EntityTameEvent and EntityBreedEvent handling.
 */
val alphaWolf = ability("alpha_wolf", "moborigins") {
    title = text("Alpha Wolf")
    description("Wolves you tame are stronger!")

    option("health_bonus", 10.0)
    option("attack_bonus", 2.0)

    // Implementation note: Requires EntityTameEvent and EntityBreedEvent handlers
    // When a wolf is tamed/bred by a player with this ability:
    // wolf.getAttribute(MAX_HEALTH).baseValue += health_bonus
    // wolf.getAttribute(ATTACK_DAMAGE).baseValue += attack_bonus
}

/**
 * Wolf Pack - gain bonuses when near wolves.
 * Legacy: Conditional AttributeModifierAbility
 *   - When >= 4 wolves within 8 blocks: MAX_HEALTH +0.04 ADD_NUMBER
 *   - (Note: Legacy value seems low, likely should be health not percentage)
 */
val wolfPack = ability("wolf_pack", "moborigins") {
    title = text("Wolf Pack")
    description("When you are near at least 4 wolves you gain speed and attack damage.")

    option("required_wolves", 4)
    option("search_radius", 8.0)
    option("health_bonus", 0.04)
    option("attribute", "MAX_HEALTH")
    option("operation", "ADD_NUMBER")

    onTick(interval = 20) { player, config ->
        val required = config.getInt("required_wolves", 4)
        val radius = config.getDouble("search_radius", 8.0)
        val nearbyWolves = player.getNearbyEntities(radius, radius, radius)
            .count { it.type == EntityType.WOLF }
        nearbyWolves >= required
    }
}

/**
 * Wolf Pack Attack - attack damage bonus from wolf pack.
 * Hidden ability that works with WolfPack.
 * Legacy: Conditional AttributeModifierAbility
 *   - When >= 4 wolves within 8 blocks: ATTACK_DAMAGE +2.0 ADD_NUMBER
 */
val wolfPackAttack = ability("wolf_pack_attack", "moborigins") {
    title = text("Wolf Pack Attack")
    description("Attack damage bonus from wolf pack.")
    visible = false

    option("required_wolves", 4)
    option("search_radius", 8.0)
    option("attack_bonus", 2.0)
    option("attribute", "ATTACK_DAMAGE")
    option("operation", "ADD_NUMBER")

    onTick(interval = 20) { player, config ->
        val required = config.getInt("required_wolves", 4)
        val radius = config.getDouble("search_radius", 8.0)
        val nearbyWolves = player.getNearbyEntities(radius, radius, radius)
            .count { it.type == EntityType.WOLF }
        nearbyWolves >= required
    }
}

/**
 * Wolf Howl - howl to buff nearby wolves and yourself.
 * Legacy: On PlayerLeftClickEvent with empty hand (and no clicked block):
 *   - Play ENTITY_WOLF_HOWL sound
 *   - Apply SPEED I and STRENGTH I for 400 ticks to player and nearby wolves
 */
val wolfHowl = ability("wolf_howl", "moborigins") {
    title = text("Howl")
    description("You can use the left click key when holding nothing to howl, and give speed and strength to nearby wolves and yourself.")

    option("cooldown_ticks", 900)
    option("effect_duration", 400)
    option("effect_radius", 5.0)

    onPrimaryAction { player, config ->
        // Only trigger with empty main hand
        if (!player.inventory.itemInMainHand.type.isAir) return@onPrimaryAction

        val duration = config.getInt("effect_duration", 400)
        val radius = config.getDouble("effect_radius", 5.0)

        // Play howl sound
        player.world.playSound(player, Sound.ENTITY_WOLF_HOWL, SoundCategory.PLAYERS, 1f, 0.5f)

        // Apply effects to player
        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, duration, 0, false, true))
        // Note: INCREASE_DAMAGE is the correct constant for Strength in Paper 1.20.1
        player.addPotionEffect(PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, 0, false, true))

        // Apply effects to nearby wolves (and the player again if they're in range)
        player.getNearbyEntities(radius, radius, radius)
            .filter { it.type == EntityType.WOLF || it === player }
            .filterIsInstance<LivingEntity>()
            .forEach { entity ->
                entity.addPotionEffect(PotionEffect(PotionEffectType.SPEED, duration, 0, false, true))
                entity.addPotionEffect(PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, 0, false, true))
            }
    }
}

/**
 * Full Moon - werewolf-like bonuses during full moon.
 * Legacy: Conditional AttributeModifierAbility
 *   - When night time AND moon phase is FULL_MOON: MOVEMENT_SPEED +0.07 ADD_NUMBER
 */
val fullMoon = ability("full_moon", "moborigins") {
    title = text("Werewolf-like")
    description("During a full moon you get Faster, Stronger, and Healthier.")

    option("speed_bonus", 0.07)
    option("attribute", "MOVEMENT_SPEED")
    option("operation", "ADD_NUMBER")

    // Conditional attribute based on moon phase
    onTick(interval = 100) { player, _ ->
        val world = player.world
        !world.isDayTime && world.moonPhase == MoonPhase.FULL_MOON
    }
}

/**
 * Full Moon Health - health bonus during full moon.
 * Hidden ability that works with FullMoon.
 * Legacy: Conditional AttributeModifierAbility
 *   - When night time AND FULL_MOON: MAX_HEALTH +4.0 ADD_NUMBER
 */
val fullMoonHealth = ability("full_moon_health", "moborigins") {
    title = text("Full Moon Health")
    description("Health bonus during full moon.")
    visible = false

    option("health_bonus", 4.0)
    option("attribute", "MAX_HEALTH")
    option("operation", "ADD_NUMBER")

    onTick(interval = 100) { player, _ ->
        val world = player.world
        !world.isDayTime && world.moonPhase == MoonPhase.FULL_MOON
    }
}

/**
 * Full Moon Attack - attack bonus during full moon.
 * Hidden ability that works with FullMoon.
 * Legacy: Conditional AttributeModifierAbility
 *   - When night time AND FULL_MOON: ATTACK_DAMAGE +2.0 ADD_NUMBER
 */
val fullMoonAttack = ability("full_moon_attack", "moborigins") {
    title = text("Full Moon Attack")
    description("Attack bonus during full moon.")
    visible = false

    option("attack_bonus", 2.0)
    option("attribute", "ATTACK_DAMAGE")
    option("operation", "ADD_NUMBER")

    onTick(interval = 100) { player, _ ->
        val world = player.world
        !world.isDayTime && world.moonPhase == MoonPhase.FULL_MOON
    }
}

/**
 * Collection of all wolf-related abilities.
 */
val wolfAbilities = listOf(
    wolfBody,
    alphaWolf,
    wolfPack,
    wolfPackAttack,
    wolfHowl,
    fullMoon,
    fullMoonHealth,
    fullMoonAttack
)
