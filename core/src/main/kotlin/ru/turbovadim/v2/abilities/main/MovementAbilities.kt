package ru.turbovadim.v2.abilities.main

import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.ability.FallDamageMode
import ru.turbovadim.v2.dsl.*

// ============================================
// MOVEMENT ABILITIES
// ============================================

/**
 * Cardinal directions for checking adjacent blocks.
 */
private val cardinalFaces = listOf(BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH)

/**
 * Climbing - can climb walls by flying near solid blocks.
 * Legacy: Climbing.kt
 *
 * The legacy implementation:
 * - Checks cardinal directions for solid blocks
 * - Enables flight when adjacent to a wall
 * - Uses persistent data to track climbing state
 * - Flight speed is 0.05f with normal fall damage
 */
val climbing = ability("climbing") {
    title = text("Climbing")
    description("You are able to climb up any kind of wall, not just ladders.")

    option("flight_speed", 0.05f)

    flight {
        speed = 0.05f
        fallDamage = FallDamageMode.NORMAL
    }

    // Check for adjacent solid blocks and enable flight
    onTick(interval = 1) { player, _ ->
        val baseBlock = player.location.block
        var hasSolidAdjacent = false
        var hasSolidAbove = false

        // Check all cardinal directions for solid blocks
        for (face in cardinalFaces) {
            if (baseBlock.getRelative(face).isSolid) {
                hasSolidAdjacent = true
            }
            if (baseBlock.getRelative(BlockFace.UP).getRelative(face).isSolid) {
                hasSolidAbove = true
            }
            if (hasSolidAdjacent) break
        }

        // Return true if adjacent to wall (enables flight condition)
        // The executor should handle allowFlight and isFlying state
        if (hasSolidAdjacent) {
            player.allowFlight = true
            // Auto-climb when near wall above and not on ground
            if (hasSolidAbove && !player.isOnGround) {
                player.isFlying = true
            }
        }
        hasSolidAdjacent
    }
}

/**
 * Tailwind - increased movement speed (attribute modifier).
 * Legacy: Tailwind.kt
 *
 * Uses attribute modifier: GENERIC_MOVEMENT_SPEED, amount: 0.2, operation: MULTIPLY_SCALAR_1
 * Note: Attribute modifiers are applied by the AbilityAttributeService.
 */
val tailwind = ability("tailwind") {
    title = text("Tailwind")
    description("You are a little bit quicker on foot than others.")

    // Attribute: GENERIC_MOVEMENT_SPEED, amount: 0.2, operation: MULTIPLY_SCALAR_1
    option("speed_multiplier", 0.2)
}

/**
 * Swim Speed - dolphin's grace effect when underwater.
 * Legacy: SwimSpeed.kt
 *
 * The legacy implementation:
 * - Stores existing non-infinite dolphin's grace effects
 * - Applies infinite dolphin's grace when underwater
 * - Restores original effects when leaving water
 * - Handles milk bucket consumption
 * - Runs every 6 ticks
 */
val swimSpeed = ability("swim_speed") {
    title = text("Fins")
    description("Your underwater speed is increased.")

    // Apply dolphin's grace when underwater
    // Uses finite duration (100 ticks) that gets refreshed every 6 ticks
    // This ensures the effect naturally expires when the ability is removed (e.g., origin change)
    onTick(interval = 6) { player, _ ->
        if (player.isUnderWater) {
            player.addPotionEffect(
                PotionEffect(
                    PotionEffectType.DOLPHINS_GRACE,
                    100, // Refreshed every 6 ticks, expires on ability removal
                    0,
                    false,
                    false
                )
            )
        } else {
            // Remove dolphin's grace when not underwater
            if (player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE)) {
                player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE)
            }
        }
        player.isUnderWater
    }
}

/**
 * Like Water - doesn't sink in water, can hover.
 * Legacy: LikeWater.kt
 *
 * The legacy implementation:
 * - Grants flight in water (not in bubble columns)
 * - Uses packet events to detect rising movement
 * - Disables flight when sneaking or sprinting in water
 * - Flight speed is 0.06f with no fall damage
 */
val likeWater = ability("like_water") {
    title = text("Like Water")
    description("When underwater, you do not sink to the ground unless you want to.")

    option("flight_speed", 0.06f)

    flight {
        speed = 0.06f
        fallDamage = FallDamageMode.NONE
    }

    // Flight is granted when in water and not in bubble column
    onTick(interval = 1) { player, _ ->
        val inWater = player.isInWater && !player.isInBubbleColumn

        if (inWater) {
            player.allowFlight = true
            // Disable flying when sneaking in water (to sink)
            if (player.isSneaking) {
                player.isFlying = false
            }
        }
        inWater
    }
}

/**
 * Sprint Jump - jump boost effect while sprinting.
 * Legacy: SprintJump.kt
 *
 * The legacy implementation:
 * - Applies jump boost (amplifier 1, duration 5) when sprinting
 * - Uses NMSInvoker.jumpBoostEffect for version compatibility
 * - Runs every tick
 */
val sprintJump = ability("sprint_jump") {
    title = text("Strong Ankles")
    description("You are able to jump higher by jumping while sprinting.")

    option("jump_boost_amplifier", 1)
    option("effect_duration", 5)

    // Apply jump boost while sprinting
    // Note: Use PotionEffectType.JUMP for Paper 1.20.1 (not JUMP_BOOST)
    onTick(interval = 1) { player, config ->
        if (player.isSprinting) {
            val amplifier = config.getInt("jump_boost_amplifier", 1)
            val duration = config.getInt("effect_duration", 5)
            player.addPotionEffect(
                PotionEffect(
                    PotionEffectType.JUMP, // Use JUMP for 1.20.1 compatibility
                    duration,
                    amplifier,
                    false,
                    false
                )
            )
        }
        true
    }
}

/**
 * Collection of all movement-related abilities.
 */
val movementAbilities = listOf(
    climbing,
    tailwind,
    swimSpeed,
    likeWater,
    sprintJump
)
