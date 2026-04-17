package ru.turbovadim.v2.abilities.monsters

import com.destroystokyo.paper.MaterialTags
import org.bukkit.block.BlockFace
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.ability.DamageResult
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text
import kotlin.math.max

/**
 * Damage-related abilities for monster origins.
 */

val doubleHealth = ability("double_health", "monsterorigins") {
    title = text("Tough Skin")
    description("You have double the health of a regular human.")

    attribute(AttributeType.MAX_HEALTH, 20.0, configKey = "extra_health")
}

val skeletonBody = ability("skeleton_body", "monsterorigins") {
    title = text("Skeletal Form")
    description("Your skeleton body means you have less health than humans.")

    attribute(AttributeType.MAX_HEALTH, -4.0, configKey = "health_reduction")
}

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

val doubleFireDamage = ability("double_fire_damage", "monsterorigins") {
    title = text("Frozen Skin")
    description("You take double damage from all sources of fire.")

    option("fire_damage_multiplier", 2.0)

    modifyDamage(
        incoming = { _, damage, cause, config ->
            if (cause in FIRE_CAUSES) {
                val multiplier = config.getDouble("fire_damage_multiplier", 2.0)
                DamageResult.Modify(damage * multiplier)
            } else {
                DamageResult.Allow
            }
        }
    )
}

private val FIRE_CAUSES = setOf(
    DamageCause.FIRE,
    DamageCause.FIRE_TICK,
    DamageCause.LAVA,
    DamageCause.HOT_FLOOR
)

val undeadMonsters = ability("undead", "monsterorigins") {
    title = text("Undead")
    description("You take extra damage from weapons enchanted with Smite.")
    visible = false

    option("smite_damage_per_level", 2.5)

    modifyDamage(
        incomingFromEntity = { _, attacker, damage, _, config ->
            val equipment = attacker.equipment ?: return@modifyDamage DamageResult.Allow
            val weapon = equipment.itemInMainHand
            val smiteLevel = weapon.getEnchantmentLevel(OriginsReforged.NMSInvoker.getSmiteEnchantment())
            if (smiteLevel <= 0) return@modifyDamage DamageResult.Allow
            val perLevel = config.getDouble("smite_damage_per_level", 2.5)
            DamageResult.Modify(damage + perLevel * smiteLevel)
        }
    )
}

val burnInDay = ability("burn_in_day", "monsterorigins") {
    title = text("Photoallergic")
    description("You burn in daylight.")

    option("fire_ticks", 60)

    onTick(interval = 1) { player, config ->
        val world = player.world
        if (world.environment != org.bukkit.World.Environment.NORMAL) return@onTick true
        if (!world.isDayTime) return@onTick true
        if (player.isInWaterOrRainOrBubbleColumn) return@onTick true

        var block = world.getHighestBlockAt(player.location)
        while ((MaterialTags.GLASS.isTagged(block) || MaterialTags.GLASS_PANES.isTagged(block))
            && block.y >= player.location.y
        ) {
            block = block.getRelative(BlockFace.DOWN)
        }
        if (block.y < player.location.y) {
            val fireTicks = config.getInt("fire_ticks", 60)
            player.fireTicks = max(player.fireTicks, fireTicks)
        }
        true
    }
}
