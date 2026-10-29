package ru.turbovadim.v2.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Entity
import org.bukkit.entity.Horse
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.AbstractHorseInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.api.OriginsApi
import ru.turbovadim.v2.dsl.AbilityBuilder
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import java.util.UUID

/**
 * Centaur abilities. [permanentHorse] keeps the player seated on a horse it owns;
 * [increasedSpeed] and [superJump] buff that horse while it is ridden.
 */

private val MOUNT_OWNER_KEY = NamespacedKey("fantasyorigins", "centaur_mount_owner")
private val PERMANENT_HORSE_KEY = Key.key("fantasyorigins", "permanent_horse")
private const val SADDLE_SLOT = 0

private val Entity.mountOwner: UUID?
    get() = persistentDataContainer.get(MOUNT_OWNER_KEY, PersistentDataType.STRING)?.let(UUID::fromString)

private fun ownedMount(player: Player): Horse? =
    (player.vehicle as? Horse)?.takeIf { it.mountOwner == player.uniqueId }

/** Clears ownership first so the dismount guard lets the rider off. */
private fun release(horse: Horse) {
    horse.persistentDataContainer.remove(MOUNT_OWNER_KEY)
    horse.remove()
}

private fun spawnMount(player: Player) {
    val horse = player.world.spawn(player.location, Horse::class.java) { horse ->
        horse.persistentDataContainer.set(MOUNT_OWNER_KEY, PersistentDataType.STRING, player.uniqueId.toString())
        horse.isPersistent = false
        horse.setAdult()
        horse.isTamed = true
        horse.owner = player
        horse.style = Horse.Style.NONE
        horse.inventory.saddle = ItemStack(Material.SADDLE)
    }
    horse.addPassenger(player)

    // Runs on the horse's region; removes it once the owner is off it (death, quit) or no longer a centaur.
    horse.scheduler.runAtFixedRate(OriginsReforged.instance, { task ->
        val rider = horse.passengers.firstOrNull { it.uniqueId == player.uniqueId } as? Player
        if (rider == null || OriginsApi.getOrNull()?.hasAbility(rider, PERMANENT_HORSE_KEY) != true) {
            task.cancel()
            release(horse)
        }
    }, null, 20L, 20L)
}

val permanentHorse = ability("permanent_horse", "fantasyorigins") {
    title = text("Half Horse")
    description("You are half horse, half human.")

    onTick(interval = 20) { player, _ ->
        if (player.isDead || player.vehicle != null || player.gameMode == GameMode.SPECTATOR) return@onTick false
        spawnMount(player)
        true
    }

    listener<EntityDismountEvent>(playerFrom = { it.entity as? Player }) { player, event, _ ->
        if (event.isCancellable && !player.isDead && event.dismounted.mountOwner == player.uniqueId) {
            event.isCancelled = true
        }
    }

    // The horse is a part of the player: its damage goes to the rider instead.
    listener<EntityDamageEvent>(
        playerFrom = { event ->
            val horse = event.entity as? Horse ?: return@listener null
            val owner = horse.mountOwner ?: return@listener null
            horse.passengers.firstOrNull { it.uniqueId == owner } as? Player
        }
    ) { player, event, _ ->
        event.isCancelled = true
        OriginsReforged.NMSInvoker.transferDamageEvent(player, event)
    }

    // A fresh horse is spawned at the destination on the next tick.
    listener<PlayerTeleportEvent>(priority = EventPriority.MONITOR, playerFrom = { it.player }) { player, _, _ ->
        ownedMount(player)?.let(::release)
    }

    listener<PlayerQuitEvent>(playerFrom = { it.player }) { player, _, _ ->
        ownedMount(player)?.let(::release)
    }

    listener<InventoryClickEvent>(playerFrom = { it.whoClicked as? Player }) { player, event, _ ->
        val inventory = event.clickedInventory as? AbstractHorseInventory ?: return@listener
        if (event.slot == SADDLE_SLOT && (inventory.holder as? Entity)?.mountOwner == player.uniqueId) {
            event.isCancelled = true
        }
    }

    listener<PlayerBedEnterEvent>(playerFrom = { it.player }) { _, event, _ ->
        event.setUseBed(Event.Result.DENY)
    }
}

/** Sets [attribute]'s base value on the player's centaur horse from [configKey]. */
private fun AbilityBuilder.mountAttribute(attribute: () -> Attribute, configKey: String, default: Double) {
    option(configKey, default)
    onTick(interval = 20) { player, config ->
        val horse = ownedMount(player) ?: return@onTick false
        horse.getAttribute(attribute())?.baseValue = config.getDouble(configKey, default)
        true
    }
}

val increasedSpeed = ability("increased_speed", "fantasyorigins") {
    title = text("Dashmaster")
    description(
        "From years of training for race after race,",
        "you're much faster than any normal horse."
    )

    mountAttribute({ OriginsReforged.NMSInvoker.movementSpeedAttribute }, "movement_speed", 0.4)
}

val superJump = ability("super_jump", "fantasyorigins") {
    title = text("Bouncing")
    description(
        "For years you've always felt as if your legs don't do quite enough,",
        "with all this training you can reach even higher heights."
    )

    mountAttribute({ OriginsReforged.NMSInvoker.horseJumpStrengthAttribute }, "jump_strength", 1.0)
}

val centaurAbilities = listOf(
    permanentHorse,
    increasedSpeed,
    superJump
)
