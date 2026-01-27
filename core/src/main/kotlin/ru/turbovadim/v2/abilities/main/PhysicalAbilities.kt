package ru.turbovadim.v2.abilities.main

import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

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
 * - Applies mining fatigue (or uses attribute modifier on 1.21+) if conditions met
 * - Does NOT apply if player has strength effect
 * - Stores and restores existing mining fatigue effects
 * - Uses infinite duration for the ability-applied effect
 */
val weakArms = ability("weak_arms") {
    title = text("Weak Arms")
    description("When not under the effect of a strength potion, you can only mine natural stone if there are at most 2 other natural stone blocks adjacent to it.")

    option("adjacent_stone_threshold", 2)

    // Break speed modifier - returns 0 if too many adjacent stones
    modifyBreakSpeed { player, baseSpeed, context, config ->
        val threshold = config.getInt("adjacent_stone_threshold", 2)

        // Check if player has strength effect (bypasses restriction)
        val hasStrength = player.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE) // STRENGTH in older versions
        if (hasStrength) {
            return@modifyBreakSpeed baseSpeed
        }

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
    modifyBreakSpeed { player, baseSpeed, context, _ ->
        val targetBlock = context.block
        val heldItem = context.tool

        // Only modify if not holding a pickaxe
        if (heldItem != null && heldItem.type.name.contains("PICKAXE")) {
            return@modifyBreakSpeed baseSpeed
        }

        // Check if targeting natural stone
        if (targetBlock.type !in naturalStones) {
            return@modifyBreakSpeed baseSpeed
        }

        // Return speed as if using iron pickaxe
        // Iron pickaxe on stone gives approximately 6x speed multiplier
        // The exact calculation depends on block hardness, but this is a good approximation
        baseSpeed * 6f
    }
}

/**
 * Unwieldy - cannot use shields.
 * Legacy: Unwieldy.kt
 *
 * The legacy implementation:
 * - Cancels USE_ITEM packet for shields
 * - Constantly sets shield cooldown via packet
 */
val unwieldy = ability("unwieldy") {
    title = text("Unwieldy")
    description("The way your hands are formed provide no way of holding a shield upright.")

    // Note: Shield blocking is handled via packet events in the executor
    // The executor intercepts USE_ITEM packets for shields and sends cooldown packets
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
val physicalAbilities = listOf(
    weakArms,
    strongArms,
    unwieldy,
    extraReach
)
