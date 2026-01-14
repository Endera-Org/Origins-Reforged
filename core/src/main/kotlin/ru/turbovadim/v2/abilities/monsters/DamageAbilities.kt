package ru.turbovadim.v2.abilities.monsters

import com.destroystokyo.paper.MaterialTags
import org.bukkit.Bukkit
import org.bukkit.block.BlockFace
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import ru.turbovadim.v2.ability.DamageResult
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.damageMultiplier
import ru.turbovadim.v2.dsl.text

/**
 * Damage-related abilities for monster origins.
 * Includes health modifiers, damage dealt/received modifications, and immunities.
 */

// ============================================
// HEALTH MODIFIERS
// ============================================

/**
 * Double health - adds 20 max health (10 hearts).
 * Uses AttributeModifier with ADD_NUMBER operation.
 */
val doubleHealth = ability("double_health", "monsterorigins") {
    title = text("Tough Skin")
    description("You have double the health of a regular human.")

    option("extra_health", 20.0)

    // Note: Attribute modifier application requires integration with AbilityManager
    // The attribute: MAX_HEALTH, amount: 20.0, operation: ADD_NUMBER
}

/**
 * Skeleton body - reduced max health.
 * Uses AttributeModifier with ADD_NUMBER operation.
 */
val skeletonBody = ability("skeleton_body", "monsterorigins") {
    title = text("Skeletal Form")
    description("Your skeleton body means you have less health than humans.")

    option("health_reduction", -4.0)

    // Note: Attribute modifier application requires integration with AbilityManager
    // The attribute: MAX_HEALTH, amount: -4.0, operation: ADD_NUMBER
}

// ============================================
// DAMAGE DEALT MODIFIERS
// ============================================

/**
 * Deal triple damage (named "double" but actually triples damage in legacy code).
 */
val doubleDamage = ability("double_damage", "monsterorigins") {
    title = text("Powerful Swings")
    description("You deal twice as much damage as a normal player.")

    option("damage_multiplier", 3.0)

    modifyDamage(
        outgoing = { _, damage, _, config ->
            val multiplier = config.getDouble("damage_multiplier", 3.0)
            DamageResult.Modify(damage * multiplier)
        }
    )
}

// ============================================
// DAMAGE RECEIVED MODIFIERS
// ============================================

/**
 * Take double fire damage.
 */
val doubleFireDamage = ability("double_fire_damage", "monsterorigins") {
    title = text("Frozen Skin")
    description("You take double damage from all sources of fire.")

    option("fire_damage_multiplier", 2.0)

    modifyDamage(
        incoming = { _, damage, cause, config ->
            val fireCauses = setOf(
                DamageCause.FIRE,
                DamageCause.FIRE_TICK,
                DamageCause.LAVA,
                DamageCause.HOT_FLOOR
            )
            if (cause in fireCauses) {
                val multiplier = config.getDouble("fire_damage_multiplier", 2.0)
                DamageResult.Modify(damage * multiplier)
            } else {
                DamageResult.Allow
            }
        }
    )
}

/**
 * Undead - takes extra damage from Smite enchantment.
 * Note: This requires access to the damager's weapon to check Smite enchantment level.
 * The handler receives damage events but needs attacker context.
 */
val undeadMonsters = ability("undead", "monsterorigins") {
    title = text("Undead")
    description("You take extra damage from weapons enchanted with Smite.")
    visible = false

    option("smite_damage_per_level", 2.5)

    // Note: Full implementation requires access to the attacking entity's weapon
    // to check for Smite enchantment. The modifyDamage incoming handler
    // doesn't provide attacker context directly. This would need event-based handling.
    // Legacy logic: event.damage + (2.5 * smiteLevel)
}

// ============================================
// ENVIRONMENTAL DAMAGE
// ============================================

/**
 * Burn in daylight.
 * Checks if player is exposed to sunlight and sets fire.
 */
val burnInDay = ability("burn_in_day", "monsterorigins") {
    title = text("Photoallergic")
    description("You burn in daylight.")

    option("fire_ticks", 60)
    option("overworld_name", "world")

    onTick(interval = 1) { player, config ->
        val overworldName = config.getString("overworld_name", "world")
        val overworld = Bukkit.getWorld(overworldName)

        // Check conditions: daytime, in overworld, not in water/rain
        val isInOverworld = player.world == overworld
        val isDay = player.world.isDayTime
        val isExposed = !player.isInWaterOrRainOrBubbleColumn

        if (isInOverworld && isDay && isExposed) {
            // Check if player is under open sky (not blocked by solid blocks/glass)
            var block = player.world.getHighestBlockAt(player.location)
            while ((MaterialTags.GLASS.isTagged(block) || MaterialTags.GLASS_PANES.isTagged(block))
                   && block.y >= player.location.y) {
                block = block.getRelative(BlockFace.DOWN)
            }

            val isUnderSky = block.y < player.location.y
            if (isUnderSky) {
                val fireTicks = config.getInt("fire_ticks", 60)
                player.fireTicks = maxOf(player.fireTicks, fireTicks)
            }
        }
        true
    }
}
