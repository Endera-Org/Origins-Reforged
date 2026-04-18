package ru.turbovadim.v2.abilities.magic

import org.bukkit.Bukkit
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.ability.DamageResult
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import kotlin.math.max

/**
 * Shadowmancer origin abilities ported from Origins-Magic (StarshooterCity).
 *
 * Includes:
 *   - invisible_in_darkness: Hidden from other players and untargetable when in low light.
 *   - dark_strength: Attack damage scales with darkness level (stronger in dark, weaker in light).
 *   - double_fire_damage: Double damage from fire/lava/hot floor.
 *   - burn_in_light: Catches fire if the ambient block light is above a threshold.
 *   - no_fire_resistance: Can never gain the Fire Resistance effect.
 */

private const val SHADOW_LIGHT_LEVEL_DEFAULT = 4

/**
 * Shadow Form - At low light levels the player becomes fully invisible
 * (hidden from every other player via [Player.hidePlayer]) and untargetable.
 *
 * Legacy also had a `hide_from_administrators` toggle. We expose it as a config
 * option but default to `true` so the original feel is preserved.
 */
val invisibleInDarkness = ability("invisible_in_darkness", "magicorigins") {
    title = text("Shadow Form")
    description("In really dark places you enter Shadow Form, where nothing see or attack you.")

    option("light_level", SHADOW_LIGHT_LEVEL_DEFAULT)
    option("hide_from_administrators", true)

    // Hide/show the player every 3 ticks based on light
    onTick(interval = 3) { player, config ->
        val threshold = config.getInt("light_level", SHADOW_LIGHT_LEVEL_DEFAULT)
        val hideFromOp = config.getBoolean("hide_from_administrators", true)
        val inShadow = player.location.block.lightLevel <= threshold

        val plugin = OriginsReforged.instance as Plugin
        Bukkit.getOnlinePlayers().forEach { viewer ->
            if (viewer.uniqueId == player.uniqueId) return@forEach
            if (!hideFromOp && viewer.isOp) return@forEach
            if (inShadow) viewer.hidePlayer(plugin, player) else viewer.showPlayer(plugin, player)
        }
        inShadow
    }

    // If a mob is already targeting the player and the player re-enters darkness, drop the target.
    listener<EntityTargetEvent>(
        ignoreCancelled = false,
        playerFrom = { it.target as? Player }
    ) { player, event, config ->
        val threshold = config.getInt("light_level", SHADOW_LIGHT_LEVEL_DEFAULT)
        if (player.location.block.lightLevel <= threshold) {
            event.isCancelled = true
            if (event.entity is Mob) (event.entity as Mob).target = null
        }
    }
}

/**
 * Dark Strength - Multiplies outgoing melee damage by `(7 - light_level) / 8.0`.
 * At light 0 the player deals +87.5% damage; at light 15 they deal -100% (no damage).
 */
val darkStrength = ability("dark_strength", "magicorigins") {
    title = text("Dark Strength")
    description("You deal more damage in the dark, but less in the light.")

    conditionalAttribute(
        type = AttributeType.ATTACK_DAMAGE,
        operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        checkInterval = 20
    ) { player, _ ->
        val light = player.location.block.lightLevel.toInt()
        -(light - 7) / 8.0
    }
}

/**
 * Double Fire Damage - any fire/lava/hot-floor damage to the player is doubled.
 */
val doubleFireDamage = ability("double_fire_damage", "magicorigins") {
    title = text("Fire Weakness")
    description("You take double damage from fire, lava, and hot floors.")
    visible = false

    option("multiplier", 2.0)

    modifyDamage(
        incoming = { _, damage, cause, config ->
            val isFire = cause == EntityDamageEvent.DamageCause.FIRE ||
                cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                cause == EntityDamageEvent.DamageCause.LAVA ||
                cause == EntityDamageEvent.DamageCause.HOT_FLOOR
            if (isFire) DamageResult.Modify(damage * config.getDouble("multiplier", 2.0))
            else DamageResult.Allow
        }
    )
}

/**
 * Creature of Darkness - Catches on fire whenever the ambient block-light level exceeds
 * the configured threshold (default 7).
 */
val burnInLight = ability("burn_in_light", "magicorigins") {
    title = text("Creature of Darkness")
    description("You catch fire in the presence of light, even in small amounts.")

    option("light_level", 7)
    option("burn_ticks", 120)

    onTick(interval = 20) { player, config ->
        val threshold = config.getInt("light_level", 7)
        val burnTicks = config.getInt("burn_ticks", 120)
        if (player.location.block.lightLevel > threshold) {
            player.fireTicks = max(player.fireTicks, burnTicks)
            true
        } else {
            false
        }
    }
}

/**
 * No Fire Resistance - Cancels any attempt to apply Fire Resistance to the player.
 */
val noFireResistance = ability("no_fire_resistance", "magicorigins") {
    title = text("No Fire Resistance")
    visible = false

    listener<EntityPotionEffectEvent>(
        ignoreCancelled = false,
        playerFrom = { it.entity as? Player }
    ) { _, event, _ ->
        if (event.newEffect?.type == PotionEffectType.FIRE_RESISTANCE) {
            event.isCancelled = true
        }
    }
}

val shadowmancerAbilities = listOf(
    invisibleInDarkness,
    darkStrength,
    doubleFireDamage,
    burnInLight,
    noFireResistance
)
