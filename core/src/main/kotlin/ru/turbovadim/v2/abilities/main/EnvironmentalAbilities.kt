package ru.turbovadim.v2.abilities.main

import com.destroystokyo.paper.MaterialTags
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.v2.ability.DamageResult
import ru.turbovadim.v2.dsl.*

// ============================================
// ENVIRONMENTAL ABILITIES
// ============================================

/**
 * Burn In Daylight - sets on fire in sunlight when not invisible.
 * Legacy: BurnInDaylight.kt
 *
 * The legacy implementation:
 * - Depends on phantomize (inverse - active when NOT phantomized)
 * - Checks if player is in normal world, during daytime
 * - Skips glass and glass panes when checking for cover
 * - Sets player on fire for 60 ticks minimum
 * - Runs every 15 ticks
 */
val burnInDaylight = ability("burn_in_daylight") {
    title = text("Photoallergic")
    description("You begin to burn in daylight if you are not invisible.")

    dependsOn = Key.key("origins", "phantomize")
    dependencyInverse = true

    option("check_interval", 15)
    option("fire_ticks", 60)

    onTick(interval = 15) { player, config ->
        val world = player.world
        val loc = player.location
        val playerY = loc.y

        // Only burn in normal overworld during daytime
        if (world.environment != World.Environment.NORMAL) return@onTick true
        if (!world.isDayTime) return@onTick true
        if (player.isInWaterOrRainOrBubbleColumn) return@onTick true

        // Find highest block, skipping glass and glass panes
        var block = world.getHighestBlockAt(loc)
        while ((MaterialTags.GLASS.isTagged(block) || MaterialTags.GLASS_PANES.isTagged(block)) && block.y >= playerY) {
            block = block.getRelative(BlockFace.DOWN)
        }

        // If the highest non-glass block is below player, they're exposed
        if (block.y < playerY) {
            val fireTicks = config.getInt("fire_ticks", 60)
            player.fireTicks = player.fireTicks.coerceAtLeast(fireTicks)
        }
        true
    }
}

/**
 * Fresh Air - can only sleep at high altitude.
 * Legacy: FreshAir.kt
 *
 * The legacy implementation:
 * - Cancels bed interaction if below height 86
 * - Only applies in the overworld
 * - Allows sleeping if it's daytime and clear weather (just resting)
 * - Shows action bar message when prevented
 */
val freshAir = ability("fresh_air") {
    title = text("Fresh Air")
    description("When sleeping, your bed needs to be at an altitude of at least 86 blocks, so you can breathe fresh air.")

    option("required_height", 86)

    // Note: Bed interaction is handled via PlayerInteractEvent in the executor
    // This provides the config and metadata for the ability
}

/**
 * Nether Spawn - spawns in the Nether by default.
 * Legacy: NetherSpawn.kt
 *
 * Note: Spawn handling is done via DefaultSpawnAbility interface.
 * The executor should handle first-spawn logic to place player in Nether.
 */
val netherSpawn = ability("nether_spawn") {
    title = text("Nether Inhabitant")
    description("Your natural spawn will be in the Nether.")

    // Spawn handling is done via DefaultSpawnAbility interface
    // The executor teleports the player to nether on first spawn
}

/**
 * Claustrophobia - gets weakness and slowness when under low ceilings.
 * Legacy: Claustrophobia.kt
 *
 * Implementation:
 * - Checks if block 2 above player is solid
 * - Builds up "stacks" over time (max 3600, starts at -200)
 * - Applies weakness and slowness with duration = stacks
 * - Stacks decrease when not under low ceiling
 * - Milk resets stacks to 0 (not below)
 */
val claustrophobia = ability("claustrophobia") {
    title = text("Claustrophobia")
    description("Being somewhere with a low ceiling for too long will weaken you and make you slower.")

    option("check_interval", 5)
    option("max_stacks", 3600)
    option("min_stacks", -200)
    option("buildup_rate", 1)
    option("recovery_rate", 1)

    // Type-safe state for tracking stacks per player
    val stacks = intState("stacks", default = -200)

    onTick(interval = 5) { player, config ->
        val blockAbove = player.location.block.getRelative(BlockFace.UP, 2)
        val maxStacks = config.getInt("max_stacks", 3600)
        val minStacks = config.getInt("min_stacks", -200)
        val buildupRate = config.getInt("buildup_rate", 1)
        val recoveryRate = config.getInt("recovery_rate", 1)

        val currentStacks = stacks[player]

        val newStacks = if (blockAbove.isSolid) {
            (currentStacks + buildupRate).coerceAtMost(maxStacks)
        } else {
            (currentStacks - recoveryRate).coerceAtLeast(minStacks)
        }
        stacks[player] = newStacks

        if (newStacks > 0) {
            player.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, newStacks, 0, true, true, true))
            player.addPotionEffect(PotionEffect(NMSInvoker.slownessEffect, newStacks, 0, true, true, true))
        }
        true
    }

    // Milk bucket resets stacks to 0 (but not below current value if already negative)
    listener<PlayerItemConsumeEvent>(
        playerFrom = { it.player }
    ) { player, event, _ ->
        if (event.item.type == Material.MILK_BUCKET) {
            val currentStacks = stacks[player]
            stacks[player] = minOf(currentStacks, 0)
        }
    }
}

/**
 * Aquatic - takes extra damage from Impaling enchantment.
 * Legacy: Aquatic.kt
 *
 * The legacy implementation:
 * - Adds 2.5 damage per Impaling level from tridents or melee weapons
 */
val aquatic = ability("aquatic") {
    title = text("Aquatic")
    description("You are considered an aquatic creature.")
    visible = false

    option("impaling_bonus_per_level", 2.5)

    // Extra damage from Impaling enchantment
    modifyDamage(
        incoming = { player, damage, _, config ->
            // Note: The executor needs to check the damager for Impaling enchantment
            // This handler is called with damage info; we need event context
            // For now, return Allow and let executor handle the enchantment check
            DamageResult.Allow
        }
    )
}

/**
 * Water Breathing - breathes underwater, drowns on land.
 * Legacy: WaterBreathing.kt
 *
 * The legacy implementation:
 * - Recovers air when underwater, in rain, or with water breathing potion
 * - Loses air when on land
 * - Uses persistent data keys for state tracking
 * - Handles turtle helmet special case
 * - Respiration enchantment reduces air loss rate
 * - Deals drowning damage when air runs out
 */
val waterBreathing = ability("water_breathing") {
    title = text("Gills")
    description("You can breathe underwater, but not on land.")

    option("air_recovery_rate", 4)
    option("land_damage", 2)

    onTick(interval = 1) { player, config ->
        val underwater = player.isUnderWater
        val inRain = player.isInRain
        val hasWaterBreathing = player.hasPotionEffect(PotionEffectType.WATER_BREATHING) ||
            player.hasPotionEffect(PotionEffectType.CONDUIT_POWER)
        val recoveryRate = config.getInt("air_recovery_rate", 4)

        if (underwater || inRain || hasWaterBreathing) {
            // Recover air when in water or rain
            val newAir = (player.remainingAir + recoveryRate).coerceAtMost(player.maximumAir)
            player.remainingAir = newAir
        } else {
            // Lose air on land
            // Check for respiration enchantment
            val respirationLevel = player.inventory.helmet
                ?.itemMeta
                ?.getEnchantLevel(NMSInvoker.respirationEnchantment) ?: 0

            // Respiration gives chance to not lose air
            val shouldLoseAir = if (respirationLevel > 0) {
                Math.random() > (respirationLevel.toDouble() / (respirationLevel + 1))
            } else {
                true
            }

            if (shouldLoseAir) {
                player.remainingAir = player.remainingAir - 1
            }

            // Deal drowning damage when air runs out
            if (player.remainingAir < -20) {
                val landDamage = config.getDouble("land_damage", 2.0)
                player.damage(landDamage)
                player.remainingAir = 0
            }
        }
        true
    }
}

/**
 * Air From Potions - drinking potions restores air.
 * Legacy: AirFromPotions.kt
 *
 * The legacy implementation restores 60 air when consuming any potion.
 */
val airFromPotions = ability("air_from_potions") {
    title = text("Hydration")
    description("Drinking potions restores some of your air.")
    visible = false

    option("air_restored", 60)

    // Handled via potion consume event
    onPotionConsume { player, _, config ->
        val airRestored = config.getInt("air_restored", 60)
        player.remainingAir = (player.remainingAir + airRestored).coerceAtMost(player.maximumAir)
        ru.turbovadim.v2.ability.PotionReactionResult.Allow
    }
}

/**
 * Collection of all environmental abilities.
 */
val environmentalAbilities = listOf(
    burnInDaylight,
    freshAir,
    netherSpawn,
    claustrophobia,
    aquatic,
    waterBreathing,
    airFromPotions
)
