package ru.turbovadim.v2.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.EnderPearl
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import ru.turbovadim.OriginsReforged
import ru.turbovadim.ShortcutUtils.isBedrockPlayer
import ru.turbovadim.v2.ability.DependencyAbility
import ru.turbovadim.v2.ability.InvisibilityCondition
import ru.turbovadim.v2.api.OriginsApi
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import ru.turbovadim.v2.dsl.toggleAbility
import ru.turbovadim.v2.event.OriginChangedEvent
import ru.turbovadim.v2.ui.ShulkerInventoryUI

// ============================================
// SPECIAL ABILITIES
// ============================================

/**
 * Phantomize - toggleable phantom form activated by left-click with empty hand.
 * Legacy: Phantomize.kt
 *
 * Implementation:
 * - Toggles phantom state on left-click with empty hand
 * - Requires food level > 6 to enable
 * - Auto-disables when food level drops to 6 or below
 * - As a DependencyAbility, other abilities can use `dependsOn` to depend on this
 */
val phantomize: DependencyAbility = toggleAbility("phantomize") {
    title = text("Phantom Form")
    description("Toggle phantom form by pressing the primary action key while holding nothing.")
    visible = false

    option("min_food_level", 6)

    // Toggle phantomize state on left-click with empty hand
    onInteract(Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK) { player, _, config ->
        // Must be holding nothing
        if (player.inventory.itemInMainHand.type != Material.AIR) {
            return@onInteract false
        }

        val minFood = config.getInt("min_food_level", 6)

        if (isEnabled(player)) {
            disable(player)
        } else if (player.foodLevel > minFood) {
            enable(player)
        }

        false // Don't cancel the event
    }

    // Auto-disable when food level drops too low
    onTick(interval = 20) { player, config ->
        val minFood = config.getInt("min_food_level", 6)
        if (player.foodLevel <= minFood && isEnabled(player)) {
            disable(player)
        }
        true
    }

    // Clean up state on origin change
    onOriginChanged { player, _, _ ->
        disable(player)
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

    // Show overlay when phantomize is enabled
    onDependencyEnabled { player, _ ->
        OriginsReforged.NMSInvoker.setWorldBorderOverlay(player, true)
    }

    // Hide overlay when phantomize is disabled OR when ability is removed (origin change)
    onDependencyDisabled { player, _ ->
        OriginsReforged.NMSInvoker.setWorldBorderOverlay(player, false)
    }
}

/**
 * Invisibility - invisible while phantomized.
 * Legacy: Invisibility.kt
 *
 * The legacy implementation makes the player invisible when phantomize is enabled.
 * Uses dependsOn to automatically activate only when phantomize (a DependencyAbility) is enabled.
 */
val invisibility = ability("invisibility") {
    title = text("Invisibility")
    description("While phantomized, you are invisible.")

    dependsOn = Key.key("origins", "phantomize")

    // Invisibility is always active when this ability is active
    // The dependency system handles checking phantomize.isEnabled(player)
    invisible(InvisibilityCondition.Always)
}

/** Key for marking no-damage ender pearls */
private val noDamagePearlKey by lazy { NamespacedKey(OriginsReforged.instance, "no-damage-pearl") }

/**
 * Throw Ender Pearl - throw ender pearl by left-clicking with empty hand.
 * Legacy: ThrowEnderPearl.kt
 *
 * Implementation:
 * - Left-click with empty hand while not looking at a block
 * - Has 1.5 second cooldown (30 ticks)
 * - The pearl does no damage on hit (marked via persistent data)
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
        val api = OriginsApi.getOrNull() ?: return@onPrimaryAction

        // Check cooldown
        if (api.hasCooldown(player, abilityKey)) return@onPrimaryAction

        // Set cooldown with ender_pearl icon
        val cooldownTicks = config.getInt("cooldown_ticks", 30)
        api.setCooldown(player, abilityKey, cooldownTicks, "ender_pearl")

        // Launch ender pearl and mark it as no-damage
        val pearl = player.launchProjectile(EnderPearl::class.java)
        pearl.persistentDataContainer.set(noDamagePearlKey, PersistentDataType.BYTE, 1)
    }

    // Cancel damage from our no-damage pearls
    listener<EntityDamageByEntityEvent>(
        playerFrom = { it.entity as? Player }
    ) { player, event, _ ->
        val damager = event.damager
        if (damager !is EnderPearl) return@listener

        // Check if this pearl is marked as no-damage
        if (damager.persistentDataContainer.has(noDamagePearlKey, PersistentDataType.BYTE)) {
            event.isCancelled = true
        }
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
 * Shulker Inventory - extra 9-slot persistent inventory backed by [ShulkerInventoryUI].
 * Legacy: ShulkerInventory.kt
 *
 * Opens via:
 *  - Java: right-click the helmet armor slot.
 *  - Bedrock: primary action with empty hand and no block target.
 */
val shulkerInventory = ability("shulker_inventory") {
    title = text("Hoarder")
    description("You have access to an additional 9 slots of inventory, which keep the items on death.")

    onPrimaryAction { player, _ ->
        if (!isBedrockPlayer(player.uniqueId)) return@onPrimaryAction
        if (player.inventory.itemInMainHand.type != Material.AIR) return@onPrimaryAction
        if (player.getTargetBlockExact(6) != null) return@onPrimaryAction
        ShulkerInventoryUI.openFor(player)
    }

    listener<InventoryClickEvent>(
        ignoreCancelled = false,
        playerFrom = { it.whoClicked as? Player }
    ) { player, event, _ ->
        if (event.isRightClick && event.slotType == InventoryType.SlotType.ARMOR && event.slot == 38) {
            event.isCancelled = true
            ShulkerInventoryUI.openFor(player)
        }
    }
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
        val api = OriginsApi.getOrNull() ?: return@onSneak

        // Check cooldown
        if (api.hasCooldown(player, abilityKey)) return@onSneak

        // Set cooldown with launch icon
        val cooldownTicks = config.getInt("cooldown_ticks", 600)
        api.setCooldown(player, abilityKey, cooldownTicks, "launch")

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
