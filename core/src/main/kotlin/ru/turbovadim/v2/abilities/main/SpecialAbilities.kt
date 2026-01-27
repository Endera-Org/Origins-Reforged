package ru.turbovadim.v2.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.entity.EnderPearl
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.bukkit.util.Vector
import ru.turbovadim.v2.ability.InvisibilityCondition
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.dsl.*
import ru.turbovadim.v2.event.OriginChangedEvent

// ============================================
// SPECIAL ABILITIES
// ============================================

/**
 * Phantomize - toggleable phantom form activated by left-click with empty hand.
 * Legacy: Phantomize.kt
 *
 * The legacy implementation:
 * - Toggles phantom state on left-click with empty hand
 * - Requires food level > 6 to enable
 * - Auto-disables when food level drops to 6 or below
 * - Fires AsyncPhantomizeToggleEvent for other abilities to react
 * - This is a DependencyAbility that other abilities check
 */
val phantomize = ability("phantomize") {
    title = text("Phantom Form")
    description("Toggle phantom form by pressing the primary action key while holding nothing.")
    visible = false

    option("min_food_level", 6)

    // Note: The phantomize state is tracked in the executor
    // Other abilities depend on this via the dependsOn key
    onPrimaryAction { player, config ->
        val minFood = config.getInt("min_food_level", 6)
        if (player.foodLevel > minFood && player.inventory.itemInMainHand.type == Material.AIR) {
            // Toggle phantomize state
            // The executor maintains the state and fires the toggle event
        }
    }

    // Auto-disable when food level drops too low
    onTick(interval = 20) { player, config ->
        val minFood = config.getInt("min_food_level", 6)
        if (player.foodLevel <= minFood) {
            // Disable phantomize if enabled
            // The executor handles this state transition
        }
        true
    }
}

/**
 * Phantomize Overlay - shows world border overlay when phantomized.
 * Legacy: PhantomizeOverlay.kt
 *
 * The legacy implementation shows a red world border overlay via packets
 * when the player is phantomized.
 */
val phantomizeOverlay = ability("phantomize_overlay") {
    title = text("Phantom Overlay")
    description("Shows a visual overlay when in phantom form.")
    visible = false

    dependsOn = Key.key("origins", "phantomize")

    // Note: Visual overlay is handled via packet events in the executor
}

/**
 * Invisibility - invisible while phantomized.
 * Legacy: Invisibility.kt
 *
 * The legacy implementation makes the player invisible when phantomize is enabled.
 */
val invisibility = ability("invisibility") {
    title = text("Invisibility")
    description("While phantomized, you are invisible.")

    dependsOn = Key.key("origins", "phantomize")

    // Note: The executor checks phantomize state to determine invisibility
    invisible(InvisibilityCondition.Custom { player ->
        // This would check the phantomize state from the executor
        // Placeholder - actual implementation needs DependencyAbility access
        false
    })
}

/**
 * Throw Ender Pearl - throw ender pearl by left-clicking with empty hand.
 * Legacy: ThrowEnderPearl.kt
 *
 * The legacy implementation:
 * - Left-click with empty hand while not looking at a block
 * - Has 1.5 second cooldown (30 ticks)
 * - The pearl does no damage on hit
 * - Uses persistent data to mark pearls as "false" (no damage)
 */
val throwEnderPearl = ability("throw_ender_pearl") {
    title = text("Teleportation")
    description("Whenever you want, you may throw an ender pearl which deals no damage, allowing you to teleport.")

    option("cooldown_ticks", 30)

    onPrimaryAction { player, config ->
        // Only activate with empty hand and no block target
        if (player.inventory.itemInMainHand.type != Material.AIR) return@onPrimaryAction
        if (player.getTargetBlockExact(6) != null) return@onPrimaryAction

        val abilityKey = Key.key("origins", "throw_ender_pearl")
        val cooldownManager = OriginsContainer.get().cooldownManager

        // Check cooldown
        if (cooldownManager.hasCooldown(player, abilityKey)) return@onPrimaryAction

        // Set cooldown with ender_pearl icon
        val cooldownTicks = config.getInt("cooldown_ticks", 30)
        cooldownManager.setCooldown(player, abilityKey, cooldownTicks, "ender_pearl")

        // Launch ender pearl
        val pearl = player.launchProjectile(EnderPearl::class.java)
        // The executor should mark this pearl as no-damage using persistent data
        // and handle the teleport without damage on hit
    }
}

/**
 * Lay Eggs - lays an egg when waking up from sleep.
 * Legacy: LayEggs.kt
 *
 * The legacy implementation:
 * - Listens for TimeSkipEvent with NIGHT_SKIP reason
 * - Checks if player is deeply sleeping
 * - Drops an egg at player location
 * - Plays chicken egg sound
 */
val layEggs = ability("lay_eggs") {
    title = text("Oviparous")
    description("Whenever you wake up in the morning, you will lay an egg.")

    // Note: This is handled via TimeSkipEvent in the executor
    // The ability is triggered when player is deeply sleeping during night skip
}

/**
 * Shulker Inventory - extra 9-slot inventory accessible by right-clicking helmet slot.
 * Legacy: ShulkerInventory.kt
 *
 * Note: This requires special UI handling and persistent storage.
 * The executor needs to create a custom inventory GUI.
 */
val shulkerInventory = ability("shulker_inventory") {
    title = text("Hoarder")
    description("You have access to an additional 9 slots of inventory, which keep the items on death.")

    // Note: Special inventory handling is done via custom events in the executor
}

/**
 * Elytra - has built-in Elytra wings, can glide without wearing elytra.
 * Legacy: Elytra.kt
 *
 * Implementation:
 * - Toggles gliding on double-jump (flight toggle)
 * - Prevents gliding from being cancelled while not on ground
 * - Does NOT grant creative flight - only elytra gliding
 */
val elytra = ability("elytra") {
    title = text("Winged")
    description("You have Elytra wings without needing to equip any.")

    // Enable allowFlight when player joins (so double-jump works)
    listener<PlayerJoinEvent>(
        playerFrom = { it.player }
    ) { player, _, _ ->
        player.allowFlight = true
    }

    // Update allowFlight when origin changes
    onOriginChanged { player, _: OriginChangedEvent, _ ->
        player.allowFlight = true
    }

    // Convert flight toggle to glide toggle
    listener<PlayerToggleFlightEvent>(
        playerFrom = { it.player }
    ) { player, event, _ ->
        if (event.isFlying) {
            event.isCancelled = true
            player.isGliding = !player.isGliding
        }
    }

    // Prevent gliding from being cancelled while not on ground
    listener<EntityToggleGlideEvent>(
        playerFrom = { it.entity as? Player }
    ) { player, event, _ ->
        @Suppress("DEPRECATION")
        val onGround = player.isOnGround
        if (!onGround && !event.isGliding) {
            event.isCancelled = true
        }
    }
}

/**
 * Launch Into Air - launch upward while gliding by sneaking.
 * Legacy: LaunchIntoAir.kt
 *
 * The legacy implementation:
 * - Triggered on sneak while gliding
 * - Has 30 second cooldown (600 ticks)
 * - Adds upward velocity of 2
 */
val launchIntoAir = ability("launch_into_air") {
    title = text("Gift of the Winds")
    description("Every 30 seconds, you are able to launch about 20 blocks up into the air.")

    option("cooldown_ticks", 600)
    option("launch_velocity", 2.0)

    onSneak { player, sneaking, config ->
        if (!sneaking) return@onSneak
        if (!player.isGliding) return@onSneak

        val abilityKey = Key.key("origins", "launch_into_air")
        val cooldownManager = OriginsContainer.get().cooldownManager

        // Check cooldown
        if (cooldownManager.hasCooldown(player, abilityKey)) return@onSneak

        // Set cooldown with launch icon
        val cooldownTicks = config.getInt("cooldown_ticks", 600)
        cooldownManager.setCooldown(player, abilityKey, cooldownTicks, "launch")

        val velocity = config.getDouble("launch_velocity", 2.0)
        player.velocity = player.velocity.add(Vector(0.0, velocity, 0.0))
    }
}

/**
 * Collection of all special abilities.
 */
val specialAbilities = listOf(
    phantomize,
    phantomizeOverlay,
    invisibility,
    throwEnderPearl,
    layEggs,
    shulkerInventory,
    elytra,
    launchIntoAir
)
