package ru.turbovadim.v2.abilities.main

import org.bukkit.Material
import org.bukkit.block.BlockFace
import ru.turbovadim.v2.ability.FallDamageMode
import ru.turbovadim.v2.dsl.*

// ============================================
// CREATURE-RELATED ABILITIES
// ============================================

/**
 * Master of Webs - can fly in cobwebs, traps enemies in webs, senses entities in webs.
 * Legacy: MasterOfWebs.kt
 *
 * The legacy implementation:
 * - Grants flight when inside cobweb (speed 0.04f, no fall damage)
 * - Places temporary cobweb on melee hit (2 second cooldown, 3 second duration)
 * - Makes non-arthropods in cobwebs glow for the web master
 * - Adds crafting recipe: 2 string -> 1 cobweb
 * - Prevents drops from temporary cobwebs
 */
val masterOfWebs = ability("master_of_webs") {
    title = text("Master of Webs")
    description("You navigate cobweb perfectly, and are able to climb in them. When you hit an enemy in melee, they get stuck in cobweb for a while. Non-arthropods stuck in cobweb will be sensed by you. You are able to craft cobweb from string.")

    option("flight_speed", 0.04f)
    option("web_trap_cooldown", 120)
    option("web_trap_duration", 60)
    option("sense_range", 16.0)

    flight {
        speed = 0.04f
        fallDamage = FallDamageMode.NONE
    }

    // Check if player is in cobweb
    onTick(interval = 1) { player, _ ->
        val block = player.location.block
        val blockAbove = block.getRelative(BlockFace.UP)

        // Check if player or their upper body is in cobweb
        val inCobweb = block.type == Material.COBWEB || blockAbove.type == Material.COBWEB ||
            // Also check adjacent blocks that overlap with player hitbox
            BlockFace.entries.any { face ->
                if (!face.isCartesian) return@any false
                val adjacent = block.getRelative(face)
                adjacent.type == Material.COBWEB && player.boundingBox.overlaps(adjacent.boundingBox)
            } || BlockFace.entries.any { face ->
                if (!face.isCartesian) return@any false
                val adjacent = blockAbove.getRelative(face)
                adjacent.type == Material.COBWEB && player.boundingBox.overlaps(adjacent.boundingBox)
            }

        if (inCobweb) {
            player.allowFlight = true
            player.isFlying = true
        }
        inCobweb
    }

    // Place temporary cobweb on attack
    onAttack { player, target, config ->
        val cooldownTicks = config.getInt("web_trap_cooldown", 120)
        val durationTicks = config.getInt("web_trap_duration", 60)

        // Only place web if target location isn't solid
        val targetBlock = target.location.block
        if (targetBlock.type.isSolid) return@onAttack

        // Note: Cooldown is handled by executor
        // Place temporary cobweb
        val location = targetBlock.location
        targetBlock.type = Material.COBWEB

        // Schedule removal after duration
        // Note: The executor should schedule this and track temporary cobwebs
        // to prevent drops when broken
    }
}

/**
 * Nine Lives - has 1 less heart of health (attribute modifier).
 * Legacy: NineLives.kt
 *
 * Uses attribute modifier: GENERIC_MAX_HEALTH, amount: -2.0, operation: ADD_NUMBER
 * Note: Attribute modifiers are applied by the AbilityAttributeService.
 */
val nineLives = ability("nine_lives") {
    title = text("Nine Lives")
    description("You have 1 less heart of health than humans.")

    // Attribute: GENERIC_MAX_HEALTH, amount: -2.0, operation: ADD_NUMBER
    option("health_reduction", -2.0)
}

/**
 * Scare Creepers - creepers are afraid and only attack if provoked.
 * Legacy: ScareCreepers.kt
 *
 * The legacy implementation:
 * - Adds a custom mob goal to creepers that makes them flee from players with this ability
 * - Creepers only target players who have attacked them
 * - Uses persistent data to track which player hit the creeper
 */
val scareCreepers = ability("scare_creepers") {
    title = text("Catlike Appearance")
    description("Creepers are scared of you and will only explode if you attack them first.")

    // Note: Creeper AI modification is handled via mob goals in the executor
    // The executor adds a flee goal to creepers when they spawn/load
    // EntityTargetLivingEntityEvent is cancelled unless the creeper was hit by the player
}

/**
 * Velvet Paws - footsteps don't cause vibrations.
 * Legacy: VelvetPaws.kt
 *
 * The legacy implementation cancels GenericGameEvent for GameEvent.STEP
 * when the entity is a player with this ability.
 */
val velvetPaws = ability("velvet_paws") {
    title = text("Velvet Paws")
    description("Your footsteps don't cause any vibrations which could otherwise be picked up by nearby lifeforms.")

    // Note: This is handled via GenericGameEvent in the executor
    // When event.event == GameEvent.STEP and entity has this ability, cancel the event
}

/**
 * Collection of all creature-related abilities.
 */
val creatureAbilities = listOf(
    masterOfWebs,
    nineLives,
    scareCreepers,
    velvetPaws
)
