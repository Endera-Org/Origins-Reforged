package ru.turbovadim.v2.abilities.main

import com.destroystokyo.paper.MaterialTags
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.inventory.ItemStack
import org.endera.enderalib.utils.async.runTaskLater
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import ru.turbovadim.v2.dsl.listener
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// ============================================
// PHYSICAL ABILITIES (Mining, Reach, etc.)
// ============================================

/**
 * List of natural stone blocks for mining abilities.
 */
private val naturalStones = setOf(
    Material.STONE,
    Material.TUFF,
    Material.GRANITE,
    Material.DIORITE,
    Material.ANDESITE,
    Material.SANDSTONE,
    Material.SMOOTH_SANDSTONE,
    Material.RED_SANDSTONE,
    Material.SMOOTH_RED_SANDSTONE,
    Material.DEEPSLATE,
    Material.BLACKSTONE,
    Material.NETHERRACK
)

/**
 * Weak Arms - mining fatigue when mining natural stone surrounded by more than 2 natural stone blocks.
 * Legacy: WeakArms.kt
 *
 * The legacy implementation:
 * - Checks if player is targeting natural stone with more than 2 adjacent natural stones
 * - Prevents the block-damage action if conditions are not met
 */
val weakArms = ability("weak_arms") {
    title = text("Weak Arms")
    description("You can only mine natural stone if there are at most 2 other natural stone blocks adjacent to it.")

    option("adjacent_stone_threshold", 2)

    // Break speed modifier - returns 0 if too many adjacent stones
    modifyBreakSpeed { _, baseSpeed, context, config ->
        val threshold = config.getInt("adjacent_stone_threshold", 2)

        // Check if target block is natural stone
        val targetBlock = context.block
        if (targetBlock.type !in naturalStones) {
            return@modifyBreakSpeed baseSpeed
        }

        // Count adjacent natural stone blocks (6 faces)
        val adjacentFaces = listOf(
            BlockFace.DOWN,
            BlockFace.UP,
            BlockFace.WEST,
            BlockFace.EAST,
            BlockFace.NORTH,
            BlockFace.SOUTH
        )
        val adjacentCount = adjacentFaces.count { face ->
            targetBlock.getRelative(face).type in naturalStones
        }

        if (adjacentCount > threshold) {
            // Apply mining fatigue (effectively stop mining)
            0f
        } else {
            baseSpeed
        }
    }
}

private data class StrongArmsDropKey(
    val playerId: UUID,
    val worldId: UUID,
    val x: Int,
    val y: Int,
    val z: Int
)

private val pendingStrongArmsDrops = ConcurrentHashMap<StrongArmsDropKey, Collection<ItemStack>>()

private fun strongArmsDropKey(event: BlockBreakEvent) = StrongArmsDropKey(
    event.player.uniqueId,
    event.block.world.uid,
    event.block.x,
    event.block.y,
    event.block.z
)

private fun strongArmsDropKey(event: BlockDropItemEvent) = StrongArmsDropKey(
    event.player.uniqueId,
    event.block.world.uid,
    event.block.x,
    event.block.y,
    event.block.z
)

/**
 * Strong Arms - can mine natural stone without a pickaxe at normal speed.
 * Legacy: StrongArms.kt
 *
 * The legacy implementation:
 * - Treats hand as iron pickaxe when mining natural stone
 * - Uses BreakSpeedModifierAbility to provide modified context
 * - Also handles drops via BlockBreakEvent to give pickaxe drops
 */
val strongArms = ability("strong_arms") {
    title = text("Strong Arms")
    description("You are strong enough to break natural stones without using a pickaxe.")

    // Break speed modifier: treats hand as iron pickaxe for natural stone
    modifyBreakSpeed { _, baseSpeed, context, _ ->
        val targetBlock = context.block
        val heldItem = context.tool ?: ItemStack(Material.AIR)

        // Only modify if not holding a pickaxe
        if (MaterialTags.PICKAXES.isTagged(heldItem.type)) {
            return@modifyBreakSpeed baseSpeed
        }

        // Check if targeting natural stone
        if (targetBlock.type !in naturalStones) {
            return@modifyBreakSpeed baseSpeed
        }

        val ironPickaxe = ItemStack(Material.IRON_PICKAXE)
        val ironSpeed = OriginsReforged.NMSInvoker.getDestroySpeed(ironPickaxe, targetBlock.type)
        val heldSpeed = OriginsReforged.NMSInvoker
            .getDestroySpeed(heldItem, targetBlock.type)
            .coerceAtLeast(0.0001f)

        // A preferred tool divides block hardness by 30; an unsuitable tool
        // divides it by 100. Include both the tool-speed and suitability ratio.
        baseSpeed * (ironSpeed / heldSpeed) * (100f / 30f)
    }

    listener<BlockBreakEvent>(
        priority = EventPriority.HIGHEST,
        playerFrom = { it.player }
    ) { player, event, _ ->
        val heldItem = player.inventory.itemInMainHand
        if (!event.isDropItems ||
            event.block.type !in naturalStones ||
            MaterialTags.PICKAXES.isTagged(heldItem.type)
        ) {
            return@listener
        }

        val key = strongArmsDropKey(event)
        pendingStrongArmsDrops[key] = event.block.getDrops(ItemStack(Material.IRON_PICKAXE), player)
        event.block.location.runTaskLater(OriginsReforged.instance, 1L) {
            pendingStrongArmsDrops.remove(key)
        }
    }

    listener<BlockDropItemEvent>(
        priority = EventPriority.HIGHEST,
        playerFrom = { it.player }
    ) { player, event, _ ->
        val drops = pendingStrongArmsDrops.remove(strongArmsDropKey(event)) ?: return@listener

        event.isCancelled = true
        drops.forEach { drop ->
            player.world.dropItemNaturally(event.block.location.add(0.5, 0.5, 0.5), drop)
        }
    }
}

/**
 * Unwieldy - cannot use shields.
 * Legacy: Unwieldy.kt
 *
 * Implementation:
 * - Cancels right-click when holding a shield
 * - Applies shield cooldown periodically to prevent blocking
 */
val unwieldy = ability("unwieldy") {
    title = text("Unwieldy")
    description("The way your hands are formed provide no way of holding a shield upright.")

    option("cooldown_ticks", 10)

    // Cancel shield use on right-click
    onInteract(Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK) { player, _, config ->
        val mainHand = player.inventory.itemInMainHand
        val offHand = player.inventory.itemInOffHand

        if (mainHand.type == Material.SHIELD || offHand.type == Material.SHIELD) {
            // Apply shield cooldown to prevent blocking
            val cooldownTicks = config.getInt("cooldown_ticks", 10)
            player.setCooldown(Material.SHIELD, cooldownTicks)
            true // Cancel the event
        } else {
            false
        }
    }

    // Periodically apply shield cooldown while holding
    onTick(interval = 5) { player, config ->
        val mainHand = player.inventory.itemInMainHand
        val offHand = player.inventory.itemInOffHand

        if (mainHand.type == Material.SHIELD || offHand.type == Material.SHIELD) {
            val cooldownTicks = config.getInt("cooldown_ticks", 10)
            player.setCooldown(Material.SHIELD, cooldownTicks)
        }
        true
    }
}

/**
 * Extra Reach - extended block and entity interaction range (attribute modifier).
 * Legacy: ExtraReach.kt
 *
 * Uses attribute modifiers (1.21+):
 * - PLAYER_BLOCK_INTERACTION_RANGE, amount: 1.5, operation: ADD_NUMBER
 * - PLAYER_ENTITY_INTERACTION_RANGE, amount: 1.5, operation: ADD_NUMBER
 *
 * Note: These attributes only exist in 1.20.5+.
 * The AbilityAttributeService handles version compatibility.
 */
val extraReach = ability("extra_reach") {
    title = text("Slender Body")
    description("You can reach blocks and entities further away.")

    attributes {
        add(AttributeType.BLOCK_INTERACTION_RANGE, 1.5, configKey = "extra_block_reach")
        add(AttributeType.ENTITY_INTERACTION_RANGE, 1.5, configKey = "extra_entity_reach")
    }
}

/**
 * Collection of all physical abilities.
 */
val noShield = ability("no_shield") {
    title = text("No Shields")
    description("You cannot block with shields.")
    listener<org.bukkit.event.player.PlayerInteractEvent>(playerFrom = { it.player }) { _, event, _ ->
        if (event.item?.type == org.bukkit.Material.SHIELD) event.setUseItemInHand(org.bukkit.event.Event.Result.DENY)
    }
}

val physicalAbilities = listOf(
    noShield,
    weakArms,
    strongArms,
    unwieldy,
    extraReach
)
