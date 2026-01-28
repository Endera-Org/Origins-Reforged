package ru.turbovadim.v2.abilities.main

import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes
import net.kyori.adventure.key.Key
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.entity.EntityExhaustionEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.v2.abilities.main.isPhantomized
import ru.turbovadim.v2.ability.FallDamageMode
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.immuneTo
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import java.util.UUID

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
private val unphasableBlocks = setOf(Material.OBSIDIAN, Material.BEDROCK, Material.CRYING_OBSIDIAN)

/** Tracks which players are currently phasing */
private val phasingPlayers = mutableSetOf<UUID>()

fun isPhantomized(player: Player): Boolean = phantomize.isEnabled(player)

/**
 * Phasing - can walk through solid blocks while phantomized.
 * Legacy: Phasing.kt
 *
 * Implementation:
 * - Depends on phantomize ability
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

    flight {
        speed = 0.1f
        fallDamage = FallDamageMode.NONE
    }

    // Cancel suffocation damage
    modifyDamage(
        incoming = immuneTo(DamageCause.SUFFOCATION)
    )

    // Handle phasing state and blindness
    onTick(interval = 1) { player, _ ->
        val uuid = player.uniqueId
        val eyeBlock = player.eyeLocation.block
        val feetBlock = player.location.block

        // Only phase when phantomized
        if (!isPhantomized(player)) {
            if (phasingPlayers.remove(uuid)) {
                NMSInvoker.setNoPhysics(player, false)
                NMSInvoker.sendPhasingGamemodeUpdate(player, player.gameMode)
            }
            return@onTick true
        }

        // Check if player is inside a solid block (needs phasing)
        val insideSolid = eyeBlock.type.isCollidable || feetBlock.type.isCollidable

        // Check for unphasable blocks
        val nearUnphasable = unphasableBlocks.contains(eyeBlock.type) ||
            unphasableBlocks.contains(feetBlock.type)

        // Enable/disable phasing based on position
        val shouldPhase = insideSolid && !nearUnphasable
        val isPhasing = phasingPlayers.contains(uuid)

        if (shouldPhase && !isPhasing) {
            // Enable phasing
            phasingPlayers.add(uuid)
            NMSInvoker.setNoPhysics(player, true)
            NMSInvoker.sendPhasingGamemodeUpdate(player, GameMode.SPECTATOR)
            player.allowFlight = true
            player.isFlying = true
        } else if (!shouldPhase && isPhasing) {
            // Disable phasing
            phasingPlayers.remove(uuid)
            NMSInvoker.setNoPhysics(player, false)
            NMSInvoker.sendPhasingGamemodeUpdate(player, player.gameMode)
        }

        // Apply blindness when inside solid blocks
        if (eyeBlock.type.isCollidable) {
            player.addPotionEffect(
                PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false)
            )
        } else {
            if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                player.removePotionEffect(PotionEffectType.BLINDNESS)
            }
        }
        true
    }

    // Clean up phasing state when origin changes
    onOriginChanged { player, _, _ ->
        val uuid = player.uniqueId
        if (phasingPlayers.remove(uuid)) {
            NMSInvoker.setNoPhysics(player, false)
            NMSInvoker.sendPhasingGamemodeUpdate(player, player.gameMode)
        }
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
    // which is called from V2Initializer
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
