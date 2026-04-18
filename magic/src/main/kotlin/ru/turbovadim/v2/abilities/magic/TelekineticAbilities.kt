package ru.turbovadim.v2.abilities.magic

import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.inventory.ItemStack
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text

/**
 * Telekinetic origin abilities ported from Origins-Magic (StarshooterCity).
 *
 * Includes:
 *   - increased_reach: Multi-ability adding +2 to block / entity interaction range,
 *                      plus a sub-ability that auto-picks-up nearby dropped items.
 *   - telekinesis: Items dropped by blocks and mobs go straight into the inventory.
 */

/**
 * Telekinetic Reach (parent) - visible wrapper for the three sub-abilities below.
 *
 * The three sub-abilities are registered separately and share the
 * `magicorigins:increased_reach` parent so UIs can group them.
 */
val increasedReach = ability("increased_reach", "magicorigins") {
    title = text("Telekinetic Reach")
    description("You can reach things much further away than normal.")

    // Pure marker; the actual reach comes from the attribute sub-abilities.
    onTick(interval = 200) { _, _ -> true }
}

/** +2 block interaction range (only active on versions where the attribute exists). */
val extraReachBlocks = ability("extra_reach_blocks", "magicorigins") {
    title = text("Extra Block Reach")
    visible = false

    option("reach_bonus", 2.0)

    attribute(
        AttributeType.BLOCK_INTERACTION_RANGE,
        2.0,
        configKey = "reach_bonus"
    )
}

/** +2 entity interaction range (only active on versions where the attribute exists). */
val extraReachEntities = ability("extra_reach_entities", "magicorigins") {
    title = text("Extra Entity Reach")
    visible = false

    option("reach_bonus", 2.0)

    attribute(
        AttributeType.ENTITY_INTERACTION_RANGE,
        2.0,
        configKey = "reach_bonus"
    )
}

/**
 * +Item pickup range - sucks in any non-delayed [Item] within 2.5 blocks every tick.
 * Fires a synthetic [EntityPickupItemEvent] so other plugins/abilities can react.
 */
val extraReachItems = ability("extra_reach_items", "magicorigins") {
    title = text("Extra Item Reach")
    visible = false

    option("pickup_radius", 2.5)

    onTickEnd(interval = 1) { player, config ->
        if (player.isDead) return@onTickEnd false
        val radius = config.getDouble("pickup_radius", 2.5)

        player.getNearbyEntities(radius, radius, radius)
            .filterIsInstance<Item>()
            .forEach { item ->
                if (!item.canPlayerPickup() || item.pickupDelay > 0) return@forEach
                pickUpItem(player, item)
            }
        true
    }
}

/**
 * Helper to mimic the legacy pickup logic: respect inventory capacity / stacking
 * rules and play the pickup animation only when the item was actually collected.
 */
private fun pickUpItem(player: Player, item: Item) {
    if (player.inventory.firstEmpty() == -1) {
        if (!player.inventory.containsAtLeast(item.itemStack, 1)) return
        val stackable = player.inventory.any { stack ->
            stack != null &&
                stack.type == item.itemStack.type &&
                stack.itemMeta == item.itemStack.itemMeta &&
                stack.amount < stack.maxStackSize
        }
        if (!stackable) return
    }

    val event = EntityPickupItemEvent(player, item, 0)
    if (!event.callEvent()) return

    val remaining: Map<Int, ItemStack> = player.inventory.addItem(item.itemStack)
    if (remaining.isEmpty()) {
        player.playPickupItemAnimation(item)
        item.remove()
    } else {
        for ((_, leftover) in remaining) {
            val picked = item.itemStack.amount - leftover.amount
            if (picked > 0) player.playPickupItemAnimation(item, picked)
            item.itemStack = leftover
        }
    }
}

/**
 * Telekinesis - All drops from blocks you break and mobs you kill go straight
 * into your inventory (or the ground if the inventory is full).
 */
val telekinesis = ability("telekinesis", "magicorigins") {
    title = text("Telekinesis")
    description("Items dropped from blocks and entities go straight into your inventory.")

    listener<BlockDropItemEvent>(
        ignoreCancelled = false,
        playerFrom = { it.player }
    ) { player, event, _ ->
        event.items.forEach { dropped ->
            val overflow = player.inventory.addItem(dropped.itemStack)
            overflow.values.forEach { leftover ->
                player.world.dropItemNaturally(player.location, leftover)
            }
        }
        event.items.clear()
    }

    listener<EntityDeathEvent>(
        ignoreCancelled = false,
        playerFrom = { event ->
            val victim = event.entity
            if (victim is Player) null else victim.killer
        }
    ) { player, event, _ ->
        event.drops.forEach { drop ->
            val overflow = player.inventory.addItem(drop)
            overflow.values.forEach { leftover ->
                player.world.dropItemNaturally(player.location, leftover)
            }
        }
        event.drops.clear()
    }
}

val telekineticAbilities = listOf(
    increasedReach,
    extraReachBlocks,
    extraReachEntities,
    extraReachItems,
    telekinesis
)
