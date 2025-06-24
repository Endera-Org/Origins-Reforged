package ru.turbovadim.abilities.main

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.AddonLoader.getTextFor
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged
import ru.turbovadim.OriginsReforged.Companion.instance
import ru.turbovadim.ShortcutUtils.isBedrockPlayer
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.database.ShulkerInventoryManager
import ru.turbovadim.events.PlayerLeftClickEvent

class ShulkerInventory : VisibleAbility, Listener {

    override fun getKey(): Key {
        return Key.key("origins:shulker_inventory")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "You have access to an additional 9 slots of inventory, which keep the items on death.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor("Hoarder", LineComponent.LineType.TITLE)

    var openedBoxKey: NamespacedKey = NamespacedKey(instance, "openedbox")

    private fun setupInv(player: Player) {
        val inventory = Bukkit.createInventory(
            player,
            InventoryType.DISPENSER,
            Component.text(getTextFor("container.shulker_inventory_power", "Shulker Inventory"))
        )
        player.openInventory(inventory)
        CoroutineScope(OriginsReforged.bukkitDispatcher).launch {
            val inv = withContext(ioDispatcher) { ShulkerInventoryManager.getInventory(player.uniqueId.toString()) }
            inv.forEach { item ->
                inventory.setItem(item.slot, item.itemStack)
            }
        }
        player.persistentDataContainer.set(openedBoxKey, OriginSwapper.BooleanPDT.BOOLEAN, true)
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        event.player.persistentDataContainer
            .set(openedBoxKey, OriginSwapper.BooleanPDT.BOOLEAN, false)
    }

    @EventHandler
    fun onPlayerLeftClick(event: PlayerLeftClickEvent) {
        val player = event.player
        if (event.hasBlock() || event.hasItem()) return
        if (!isBedrockPlayer(player.uniqueId)) return

        runForAbility(player) { p ->
            setupInv(p)
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        runForAbility(player) { p ->
            if (event.isRightClick && event.slotType == InventoryType.SlotType.ARMOR && event.slot == 38) {
                event.isCancelled = true
                setupInv(p)
                return@runForAbility
            }
            saveInv(p)
        }
    }

    private fun saveInv(player: Player) {
        if (player.persistentDataContainer.get(openedBoxKey, OriginSwapper.BooleanPDT.BOOLEAN) != true) return

        try {
            val inventory = player.openInventory

            if (inventory.type == InventoryType.DISPENSER) {
                Bukkit.getScheduler().runTaskLater(instance, Runnable {
                    val items = (0..8).map { ShulkerInventoryManager.SlotItem(it, inventory.getItem(it)) }

                    CoroutineScope(ioDispatcher).launch {
                        ShulkerInventoryManager.saveInventory(player.uniqueId.toString(), items)
                    }
                }, 1L)
            }
        } catch (e: Exception) {
            instance.logger.warning("Error saving shulker inventory: ${e.message}")
            e.printStackTrace()
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        saveInv(player)
    }
}