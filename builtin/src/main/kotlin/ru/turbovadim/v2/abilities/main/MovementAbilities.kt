package ru.turbovadim.v2.abilities.main

import ru.turbovadim.v2.util.refreshPotionEffect

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import org.bukkit.GameMode
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.event.player.PlayerToggleSprintEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import java.time.Instant

// ============================================
// MOVEMENT ABILITIES
// ============================================

/**
 * Cardinal directions for checking adjacent blocks.
 */
private val cardinalFaces = listOf(BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH)

private fun hasAdjacentSolidBlock(player: Player, yOffset: Int = 0): Boolean {
    val block = if (yOffset == 0) {
        player.location.block
    } else {
        player.location.block.getRelative(BlockFace.UP, yOffset)
    }
    return cardinalFaces.any { block.getRelative(it).isSolid }
}

/**
 * Climbing - can climb walls by flying only while next to a solid block.
 * Legacy: Climbing.kt
 *
 * Behavior:
 * - Only grants flight while adjacent to a solid block (so you can't free-fly).
 * - Auto-engages flight when there's a wall above and the player is airborne,
 *   unless the player has manually stopped climbing (by pressing jump mid-climb).
 * - Resets the manual-stop flag when the player lands on the ground.
 * - Prevents immediate flight cancellation within a short window after a jump,
 *   so the initial jump-to-start-climbing keystroke doesn't toggle flight off.
 */
val climbing = ability("climbing") {
    title = text("Climbing")
    description("You are able to climb up any kind of wall, not just ladders.")

    option("jump_stop_cooldown_seconds", 2)

    val stoppedClimbing = boolState("stopped_climbing", false)
    val lastJumpEpochSeconds = longState("last_jump_epoch_seconds", 0L)

    flight {
        speed = 0.05f
        fallDamage = ru.turbovadim.v2.ability.FallDamageMode.NORMAL
        condition { player, _ -> hasAdjacentSolidBlock(player) }
    }

    onTick(interval = 1) { player, _ ->
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) {
            return@onTick false
        }

        val hasSolidAdjacent = hasAdjacentSolidBlock(player)
        val hasSolidAbove = hasAdjacentSolidBlock(player, 1)

        if (player.isOnGround && stoppedClimbing[player]) {
            stoppedClimbing[player] = false
        }

        if (hasSolidAdjacent && hasSolidAbove && player.allowFlight &&
            !player.isOnGround && !stoppedClimbing[player]
        ) {
            player.isFlying = true
        }
        hasSolidAdjacent
    }

    listener<PlayerToggleFlightEvent>(
        playerFrom = { it.player }
    ) { player, event, config ->
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) {
            return@listener
        }

        if (!event.isFlying) {
            val cooldownSeconds = config.getInt("jump_stop_cooldown_seconds", 2).toLong()
            val lastJump = lastJumpEpochSeconds[player]
            if (lastJump != 0L && (Instant.now().epochSecond - lastJump) < cooldownSeconds) {
                event.isCancelled = true
                return@listener
            }
        }
        stoppedClimbing[player] = !event.isFlying
    }

    listener<PlayerJumpEvent>(
        playerFrom = { it.player }
    ) { player, _, _ ->
        lastJumpEpochSeconds[player] = Instant.now().epochSecond
    }

    onDependencyDisabled { player, _ ->
        stoppedClimbing.reset(player)
        lastJumpEpochSeconds.reset(player)
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

    attribute(
        AttributeType.MOVEMENT_SPEED,
        0.2,
        AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        configKey = "speed_multiplier"
    )
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
            player.refreshPotionEffect(
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

    flight {
        speed = 0.06f
        condition { player, _ -> player.isInWater && !player.isInBubbleColumn }
    }

    onTick(interval = 1) { player, _ ->
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) {
            return@onTick false
        }

        val inWater = player.isInWater && !player.isInBubbleColumn
        if (inWater && (player.isSneaking || player.isSprinting)) {
            player.isFlying = false
        }
        inWater
    }

    listener<PlayerMoveEvent>(
        playerFrom = { it.player }
    ) { player, event, _ ->
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) {
            return@listener
        }

        val to = event.to
        val inWater = player.isInWater && !player.isInBubbleColumn
        if (!inWater || player.isSwimming) {
            return@listener
        }

        val rising = to.y > event.from.y
        val shouldFly = player.isFlying || rising
        if (shouldFly == player.isFlying || !player.allowFlight) {
            return@listener
        }

        player.isFlying = shouldFly
    }

    listener<PlayerToggleSneakEvent>(
        playerFrom = { it.player }
    ) { player, _, _ ->
        if (player.isInWater) {
            player.isFlying = false
        }
    }

    listener<PlayerToggleSprintEvent>(
        playerFrom = { it.player }
    ) { player, _, _ ->
        if (player.isInWater && player.isFlying) {
            player.isFlying = false
        }
    }

    listener<PlayerToggleFlightEvent>(
        playerFrom = { it.player }
    ) { player, event, _ ->
        if (player.isInWater) {
            event.isCancelled = true
        }
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
            player.refreshPotionEffect(
                PotionEffect(
                    PotionEffectType.JUMP_BOOST,
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
