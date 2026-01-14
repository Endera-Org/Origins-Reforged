package ru.turbovadim.v2.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.Particle
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.ability.FallDamageMode
import ru.turbovadim.v2.dsl.*

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
 * The legacy implementation multiplies exhaustion from EntityExhaustionEvent by 1.6.
 * Note: This requires event handling in the executor.
 */
val moreExhaustion = ability("more_exhaustion") {
    title = text("Large Appetite")
    description("You exhaust much quicker than others, thus requiring you to eat more.")

    option("exhaustion_multiplier", 1.6f)

    // Note: The executor handles EntityExhaustionEvent and multiplies exhaustion
    // event.exhaustion = event.exhaustion * 1.6f
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

    option("frequency", 4)

    particles(interval = 4) { player, _ ->
        player.world.spawnParticle(
            Particle.FLAME,
            player.location.add(0.0, 1.0, 0.0),
            1,
            0.3, 0.5, 0.3,
            0.01
        )
    }
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

    option("frequency", 4)

    particles(interval = 4) { player, _ ->
        player.world.spawnParticle(
            Particle.PORTAL,
            player.location.add(0.0, 1.0, 0.0),
            1,
            0.3, 0.5, 0.3,
            0.01
        )
    }
}

/**
 * Phasing - can walk through solid blocks while phantomized.
 * Legacy: Phasing.kt
 *
 * The legacy implementation:
 * - Depends on phantomize ability
 * - Sends SPECTATOR gamemode packet to allow no-clip
 * - Enables flight while phasing (speed 0.1f, no fall damage)
 * - Cancels suffocation damage
 * - Applies blindness when inside solid blocks
 * - Cannot phase through obsidian or bedrock
 * - Uses BreakSpeedModifierAbility to allow mining while phased
 */
val phasing = ability("phasing") {
    title = text("Phasing")
    description("While phantomized, you can walk through solid material, except Obsidian.")

    dependsOn = Key.key("origins", "phantomize")

    option("flight_speed", 0.1f)
    option("unphasable_blocks", listOf("OBSIDIAN", "BEDROCK"))

    flight {
        speed = 0.1f
        fallDamage = FallDamageMode.NONE
    }

    // Cancel suffocation damage
    modifyDamage(
        incoming = immuneTo(DamageCause.SUFFOCATION)
    )

    // Apply blindness when inside solid blocks, enable phasing when sneaking on ground
    onTick(interval = 1) { player, _ ->
        // Note: Full phasing requires NMS calls (setNoPhysics, sendPhasingGamemodeUpdate)
        // The executor handles the complex state management

        // Apply blindness if eye location is in a solid block
        val eyeBlock = player.eyeLocation.block
        if (eyeBlock.type.isCollidable) {
            if (!player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                player.addPotionEffect(
                    PotionEffect(PotionEffectType.BLINDNESS, -1, 0, false, false)
                )
            }
        } else {
            if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                player.removePotionEffect(PotionEffectType.BLINDNESS)
            }
        }
        true
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
 * The legacy implementation adds a shapeless recipe: 2 string -> 1 cobweb.
 * Note: Recipe registration is handled by the executor on plugin load.
 */
val webbing = ability("webbing") {
    title = text("Webbing")
    description("You are able to craft cobweb from string.")
    visible = false

    // Note: The executor registers the crafting recipe on startup
    // ShapelessRecipe: 2x STRING -> 1x COBWEB
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
