package ru.turbovadim.abilities.fantasy

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.entity.Horse
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsReforged
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.OriginsReforged.Companion.bukkitDispatcher
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.events.PlayerSwapOriginEvent
import ru.turbovadim.packetsenders.FantasyEntityDismountEvent
import ru.turbovadim.packetsenders.FantasyEntityMountEvent

class PermanentHorse : VisibleAbility, Listener {

    override val description: MutableList<OriginSwapper.LineData.LineComponent> =
        OriginSwapper.LineData.makeLineFor(
            "You are half horse, half human.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )


    override val title: List<OriginSwapper.LineData.LineComponent> =
        OriginSwapper.LineData.makeLineFor("Half Horse", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override val key = Key.key("fantasyorigins:permanent_horse")


    @EventHandler
    fun onEntityDismount(event: FantasyEntityDismountEvent) {
        if (event.getEntity().isDead) return
        runForAbility(event.entity) {
            event.setCancelled(true)
            val vehicle = event.dismounted.vehicle
            vehicle?.removePassenger(event.dismounted)
        }
    }

    @EventHandler
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        val player = event.player
        if (player.persistentDataContainer.has(teleportingKey)) return

        runForAbility(player) {
            player.vehicle?.let { vehicle ->
                vehicle.removePassenger(player)
                player.persistentDataContainer.set(teleportingKey, PersistentDataType.BOOLEAN, true)
                event.isCancelled = true

                Bukkit.getScheduler().scheduleSyncDelayedTask(OriginsReforged.instance) {
                    player.teleport(event.to, event.cause)
                    vehicle.teleport(event.to, event.cause)
                    vehicle.addPassenger(player)
                    player.persistentDataContainer.remove(teleportingKey)
                }
            }
        }
    }

    @EventHandler
    fun onEntityMount(event: FantasyEntityMountEvent) {
        val entity = event.entity
        runForAbility(entity) {
            val mountOwner = event.mount.persistentDataContainer.getOrDefault(mountKey, PersistentDataType.STRING, "")
            if (mountOwner != entity.uniqueId.toString()) {
                event.isCancelled = true
                if (event.mount !is LivingEntity) {
                    entity.vehicle?.let { vehicle ->
                        event.mount.addPassenger(vehicle)
                    }
                }
            }
        }
    }

    private val mountKey = NamespacedKey(OriginsReforged.instance, "mount-key")
    private val teleportingKey = NamespacedKey(OriginsReforged.instance, "teleporting")

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        if (event.tickNumber % 20 != 0) return

        CoroutineScope(ioDispatcher).launch {
            Bukkit.getOnlinePlayers()
                .filter { !it.isDead }
                .forEach { player ->
                    runForAbilityAsync(player) {
                        if (player.persistentDataContainer.has(teleportingKey) || player.vehicle != null) return@runForAbilityAsync

                        val horse = player.world.spawnEntity(player.location, EntityType.HORSE) as Horse
                        val jumpAttr = horse.getAttribute(NMSInvoker.genericJumpStrengthAttribute)
                        val speedAttr = horse.getAttribute(NMSInvoker.movementSpeedAttribute)

                        OriginSwapper.getOrigin(player, "origin")?.let { origin ->
                            if (origin.hasAbility(Key.key("fantasyorigins:super_jump"))) {
                                jumpAttr?.baseValue = 1.0
                            }
                            if (origin.hasAbility(Key.key("fantasyorigins:increased_speed"))) {
                                speedAttr?.baseValue = 0.4
                            }
                        }

                        horse.persistentDataContainer.set(mountKey, PersistentDataType.STRING, player.uniqueId.toString())
                        horse.isTamed = true
                        horse.style = Horse.Style.NONE


                        launch(bukkitDispatcher) {
                            val saddle = ItemStack(Material.SADDLE).apply {
                                itemMeta = itemMeta?.apply {
                                    persistentDataContainer.set(mountKey, OriginSwapper.BooleanPDT.BOOLEAN, true)
                                }
                            }
                            horse.inventory.saddle = saddle
                            horse.addPassenger(player)
                        }
                    }
                }
        }
    }


    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.getCurrentItem() == null) return
        if (event.getCurrentItem()!!.itemMeta == null) return
        if (event.getCurrentItem()!!.itemMeta.persistentDataContainer.has(mountKey)) event.isCancelled = true
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        runForAbility(event.entity) {
            val vehicle = event.getEntity().vehicle
            if (vehicle != null && vehicle.persistentDataContainer.has(mountKey)) vehicle.remove()
        }
    }

    @EventHandler
    fun onPlayerSwapOrigin(event: PlayerSwapOriginEvent) {
        val vehicle = event.getPlayer().vehicle
        if (vehicle != null && vehicle.persistentDataContainer.has(mountKey)) vehicle.remove()
    }

    @EventHandler
    fun onPlayerBedEnter(event: PlayerBedEnterEvent) {
        runForAbility(event.player) {
            event.setUseBed(Event.Result.DENY)
        }
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        if (event.getEntity().persistentDataContainer.has(mountKey)) {
            for (entity in event.getEntity().passengers) {
                if (entity is LivingEntity) {
                    NMSInvoker.transferDamageEvent(entity, event)
                }
            }
            event.isCancelled = true
        }
    }
}
