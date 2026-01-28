package ru.turbovadim.v2.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.BlockFace
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.persistence.PersistentDataType
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.ability.FallDamageMode
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import java.util.concurrent.ConcurrentHashMap

// ============================================
// CREATURE-RELATED ABILITIES
// ============================================

/** Tracks temporary cobweb locations to prevent drops when broken */
private val temporaryCobwebs = ConcurrentHashMap.newKeySet<Location>()

/**
 * Master of Webs - can fly in cobwebs, traps enemies in webs, senses entities in webs.
 * Legacy: MasterOfWebs.kt
 *
 * Implementation:
 * - Grants flight when inside cobweb (speed 0.04f, no fall damage)
 * - Places temporary cobweb on melee hit (2 second cooldown, 3 second duration)
 * - Prevents drops from temporary cobwebs
 * - Crafting recipe: 2 string -> 1 cobweb (handled in WebbingRecipe.kt)
 */
val masterOfWebs = ability("master_of_webs") {
    title = text("Master of Webs")
    description("You navigate cobweb perfectly, and are able to climb in them. When you hit an enemy in melee, they get stuck in cobweb for a while. Non-arthropods stuck in cobweb will be sensed by you. You are able to craft cobweb from string.")

    option("flight_speed", 0.04f)
    option("web_trap_cooldown", 40)
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
        val abilityKey = Key.key("origins", "master_of_webs")
        val cooldownManager = OriginsContainer.get().cooldownManager

        // Check cooldown
        if (cooldownManager.hasCooldown(player, abilityKey)) return@onAttack

        val cooldownTicks = config.getInt("web_trap_cooldown", 40)
        val durationTicks = config.getInt("web_trap_duration", 60)

        // Only place web if target location isn't solid and is air
        val targetBlock = target.location.block
        if (targetBlock.type != Material.AIR) return@onAttack

        // Set cooldown
        cooldownManager.setCooldown(player, abilityKey, cooldownTicks, "cobweb")

        // Place temporary cobweb
        val location = targetBlock.location.clone()
        targetBlock.type = Material.COBWEB
        temporaryCobwebs.add(location)

        // Schedule removal after duration
        Bukkit.getScheduler().runTaskLater(OriginsReforged.instance, Runnable {
            if (targetBlock.type == Material.COBWEB) {
                targetBlock.type = Material.AIR
            }
            temporaryCobwebs.remove(location)
        }, durationTicks.toLong())
    }

    // Prevent drops from temporary cobwebs
    listener<BlockBreakEvent>(
        playerFrom = { it.player }
    ) { _, event, _ ->
        val blockLocation = event.block.location
        if (temporaryCobwebs.contains(blockLocation)) {
            event.isDropItems = false
            temporaryCobwebs.remove(blockLocation)
        }
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

    attribute(AttributeType.MAX_HEALTH, -2.0, configKey = "health_reduction")
}

/** Key for storing which player hit a creeper */
private val hitByPlayerKey by lazy { NamespacedKey(OriginsReforged.instance, "hit-by-player") }

/**
 * Scare Creepers - creepers are afraid and only attack if provoked.
 * Legacy: ScareCreepers.kt
 *
 * Implementation:
 * - Creepers will not target players with this ability unless attacked first
 * - When a player attacks a creeper, the player's UUID is stored on the creeper
 * - Uses persistent data so it survives server restarts
 */
val scareCreepers = ability("scare_creepers") {
    title = text("Catlike Appearance")
    description("Creepers are scared of you and will only explode if you attack them first.")

    // Cancel creeper targeting unless player attacked the creeper first
    onEntityTarget { player, attacker, _ ->
        if (attacker.type != EntityType.CREEPER) return@onEntityTarget true // Allow non-creepers

        // Check if this player hit this creeper
        val storedUuid = attacker.persistentDataContainer.get(hitByPlayerKey, PersistentDataType.STRING)
        val wasAttackedByThisPlayer = storedUuid == player.uniqueId.toString()

        // Return true to allow targeting, false to cancel
        wasAttackedByThisPlayer
    }

    // Track when player attacks a creeper - store player UUID on the creeper
    listener<EntityDamageByEntityEvent>(
        playerFrom = { event ->
            if (event.entity.type != EntityType.CREEPER) return@listener null
            when (val damager = event.damager) {
                is Player -> damager
                is Projectile -> damager.shooter as? Player
                else -> null
            }
        }
    ) { player, event, _ ->
        val creeper = event.entity
        // Store which player hit this creeper
        creeper.persistentDataContainer.set(hitByPlayerKey, PersistentDataType.STRING, player.uniqueId.toString())
    }
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
