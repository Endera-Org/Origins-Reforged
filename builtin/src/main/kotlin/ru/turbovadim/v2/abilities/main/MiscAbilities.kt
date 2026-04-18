package ru.turbovadim.v2.abilities.main

import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes
import net.kyori.adventure.key.Key
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.entity.EntityExhaustionEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.endera.enderalib.utils.async.runTask
import ru.turbovadim.OriginsReforged
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.v2.ability.StateKey
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.immuneTo
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text

// ============================================
// MISCELLANEOUS ABILITIES
// ============================================

/**
 * Hunger Over Time - exhaustion increases while phantomized.
 * Legacy: HungerOverTime.kt
 *
 * The legacy implementation:
 * - Depends on phantomize ability
 * - Adds 0.812f exhaustion every 20 ticks (1 second)
 * - Only active when phantomized
 */
val hungerOverTime = ability("hunger_over_time") {
    title = text("Fast Metabolism")
    description("Being phantomized causes you to become hungry.")

    dependsOn = Key.key("origins", "phantomize")

    option("exhaustion_per_second", 0.812f)

    onTick(interval = 20) { player, config ->
        val exhaustion = config.getFloat("exhaustion_per_second", 0.812f)
        player.exhaustion += exhaustion
        true
    }
}

/**
 * More Exhaustion - exhaustion from actions is increased.
 * Legacy: MoreExhaustion.kt
 *
 * Implementation: Multiplies exhaustion from EntityExhaustionEvent by 1.6.
 */
val moreExhaustion = ability("more_exhaustion") {
    title = text("Large Appetite")
    description("You exhaust much quicker than others, thus requiring you to eat more.")

    option("exhaustion_multiplier", 1.6f)

    listener<EntityExhaustionEvent>(
        playerFrom = { it.entity as? Player }
    ) { _, event, config ->
        val multiplier = config.getFloat("exhaustion_multiplier", 1.6f)
        event.exhaustion *= multiplier
    }
}

/**
 * Flame Particles - spawns flame particles around the player.
 * Legacy: FlameParticles.kt
 *
 * The legacy implementation spawns FLAME particles every 4 ticks.
 */
val flameParticles = ability("flame_particles") {
    title = text("Fiery Aura")
    description("Flame particles appear around you.")
    visible = false

    particles(
        particleType = ParticleTypes.FLAME,
        frequency = 4,
        offsetX = 0.3f,
        offsetY = 0.5f,
        offsetZ = 0.3f,
        count = 1
    )
}

/**
 * Ender Particles - spawns portal particles around the player.
 * Legacy: EnderParticles.kt
 *
 * The legacy implementation spawns PORTAL particles every 4 ticks.
 */
val enderParticles = ability("ender_particles") {
    title = text("Ender Aura")
    description("Portal particles appear around you.")
    visible = false

    particles(
        particleType = ParticleTypes.PORTAL,
        frequency = 4,
        offsetX = 0.3f,
        offsetY = 0.5f,
        offsetZ = 0.3f,
        count = 1
    )
}

/** Blocks that cannot be phased through */
private val unphasableBlocks = listOf(Material.OBSIDIAN, Material.BEDROCK, Material.CRYING_OBSIDIAN)

/** State key for tracking phasing status - defined at file level for access from lifecycle callbacks */
private val isPhasingState = StateKey(
    Key.key("origins", "phasing"),
    "active",
    false,
    Boolean::class
)

fun isPhantomized(player: Player): Boolean = phantomize.isEnabled(player)

/**
 * Check if entity is inside a solid block (excluding unphasable blocks).
 * Uses multiple offset checks like the legacy implementation.
 */
private fun isInSolidBlock(player: Player): Boolean {
    val location = player.location
    val offsets = listOf(0.4, -0.4)
    val checkLocations = listOf(
        location.clone().add(0.0, 1.0, 0.0), // Eye level
        location.clone() // Feet level
    )

    return checkLocations.any { base ->
        offsets.any { dx ->
            offsets.any { dz ->
                val block = base.clone().add(dx, 0.0, dz).block
                block.type.isSolid && block.type !in unphasableBlocks
            }
        }
    }
}

/**
 * Phasing - can walk through solid blocks while phantomized.
 * Legacy: Phasing.kt
 *
 * Implementation:
 * - Depends on phantomize ability
 * - Activates when sneaking on ground OR when inside a solid block
 * - Sends SPECTATOR gamemode packet to allow no-clip
 * - Enables flight while phasing (speed 0.1f, no fall damage)
 * - Cancels suffocation damage
 * - Applies blindness when inside solid blocks
 * - Cannot phase through obsidian or bedrock
 */
val phasing = ability("phasing") {
    title = text("Phasing")
    description("While phantomized, you can walk through solid material, except Obsidian.")

    dependsOn = Key.key("origins", "phantomize")

    option("flight_speed", 0.1f)

    // NO unconditional flight - flight is granted manually only when actively phasing

    // Cancel suffocation damage (only when phantomized due to dependsOn)
    modifyDamage(
        incoming = immuneTo(DamageCause.SUFFOCATION)
    )

    // Handle phasing state and blindness (runs at end of each tick, after movement processing)
    onTickEnd { player, config ->
        val inBlock = isInSolidBlock(player)
        val blockBelow = player.location.block.getRelative(BlockFace.DOWN).type

        // Phasing activates when: (on ground AND sneaking AND block below is phasable) OR already in a solid block
        @Suppress("DEPRECATION")
        val shouldPhase = (player.isOnGround && player.isSneaking && blockBelow !in unphasableBlocks) || inBlock

        val currentlyPhasing = isPhasingState[player]

        // Update phasing state
        if (shouldPhase != currentlyPhasing) {
            isPhasingState[player] = shouldPhase

            if (shouldPhase) {
                // Enable phasing - send spectator gamemode packet
                val currentVelocity = player.velocity
                NMSInvoker.sendPhasingGamemodeUpdate(player, GameMode.SPECTATOR)
                // Restore velocity after gamemode packet (entity-tied: follows player across regions)
                player.runTask(OriginsReforged.instance) { player.velocity = currentVelocity }
                // Enable flight for phasing
                player.allowFlight = true
                player.flySpeed = config.getFloat("flight_speed", 0.1f)
            } else {
                // Disable phasing - restore normal gamemode
                NMSInvoker.sendPhasingGamemodeUpdate(player, player.gameMode)
                // Disable flight when not phasing (unless in creative/spectator)
                if (player.gameMode != GameMode.CREATIVE && player.gameMode != GameMode.SPECTATOR) {
                    player.allowFlight = false
                    player.isFlying = false
                }
            }
        }

        // Always update no-physics based on current state
        val phasingActive = isPhasingState[player]
        NMSInvoker.setNoPhysics(player, player.gameMode == GameMode.SPECTATOR || phasingActive)

        // Handle flight and fall damage when phasing
        if (phasingActive) {
            player.fallDistance = 0f
            player.isFlying = true
            // Enforce flight speed every tick to prevent scroll wheel changes
            player.flySpeed = config.getFloat("flight_speed", 0.1f)
        }

        // Apply/remove blindness based on eye position
        val eyeBlock = player.eyeLocation.block
        if (eyeBlock.type.isCollidable && phasingActive) {
            player.addPotionEffect(
                PotionEffect(PotionEffectType.BLINDNESS, -1, 0, false, false)
            )
        } else {
            player.removePotionEffect(PotionEffectType.BLINDNESS)
        }

        true
    }

    // Block movement into unphasable blocks
    listener<PlayerMoveEvent>(
        playerFrom = { it.player }
    ) { player, event, _ ->
        if (isPhasingState[player]) {
            val to = event.to ?: return@listener
            val offsets = listOf(0.4, -0.4)
            val checkLocations = listOf(to.clone().add(0.0, 1.0, 0.0), to.clone())

            val movingIntoUnphasable = checkLocations.any { base ->
                offsets.any { dx ->
                    offsets.any { dz ->
                        base.clone().add(dx, 0.0, dz).block.type in unphasableBlocks
                    }
                }
            }

            if (movingIntoUnphasable) {
                event.isCancelled = true
            }
        }
    }

    // Clean up phasing state when phantomize is disabled
    onDependencyDisabled { player, _ ->
        if (isPhasingState[player]) {
            isPhasingState[player] = false
            NMSInvoker.setNoPhysics(player, false)
            NMSInvoker.sendPhasingGamemodeUpdate(player, player.gameMode)
        }
        // Disable flight (unless in creative/spectator)
        if (player.gameMode != GameMode.CREATIVE && player.gameMode != GameMode.SPECTATOR) {
            player.allowFlight = false
            player.isFlying = false
        }
        player.removePotionEffect(PotionEffectType.BLINDNESS)
    }
}

/**
 * Aqua Affinity - can mine underwater at normal speed.
 * Legacy: AquaAffinity.kt
 *
 * The legacy implementation uses BreakSpeedModifierAbility to provide
 * a context that treats the player as if they have aqua affinity enchantment.
 */
val aquaAffinity = ability("aqua_affinity") {
    title = text("Aqua Affinity")
    description("You may break blocks underwater as others do on land.")

    // Break speed modifier: treats underwater mining as if on land
    modifyBreakSpeed { player, baseSpeed, context, _ ->
        if (context.isUnderwater) {
            // Return normal break speed (cancel underwater penalty)
            baseSpeed * 5f // Underwater penalty is normally 5x slower
        } else {
            baseSpeed
        }
    }
}

/**
 * Webbing - can craft cobwebs from string.
 * Legacy: Webbing.kt / Part of MasterOfWebs.kt
 *
 * Adds a shapeless recipe: 2 string -> 1 cobweb.
 * Recipe is registered globally when the ability system initializes.
 */
val webbing = ability("webbing") {
    title = text("Webbing")
    description("You are able to craft cobweb from string.")
    visible = false

    // Recipe registration is handled in WebbingRecipe.register()
    // which is called from the built-in registration module.
}

/**
 * Collection of all miscellaneous abilities.
 */
val miscAbilities = listOf(
    hungerOverTime,
    moreExhaustion,
    flameParticles,
    enderParticles,
    phasing,
    aquaAffinity,
    webbing
)

// ============================================
// MASTER ABILITY LIST
// ============================================

/**
 * All v2 abilities combined into one list for easy registration.
 */
val allAbilities = damageAbilities +
    visionAbilities +
    foodAbilities +
    movementAbilities +
    environmentalAbilities +
    combatAbilities +
    specialAbilities +
    creatureAbilities +
    physicalAbilities +
    miscAbilities
