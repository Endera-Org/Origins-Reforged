package ru.turbovadim.v2.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.OriginsReforged
import ru.turbovadim.OriginsReforged.Companion.bukkitDispatcher
import ru.turbovadim.database.ShulkerInventoryManager

object ShulkerInventoryUI : Listener {

    private const val SLOTS = 9

    /**
     * Custom holder used to identify our inventory in close events without
     * conflicting with any other plugin inventories.
     */
    private class ShulkerInventoryHolder : InventoryHolder {
        lateinit var backing: Inventory
        override fun getInventory(): Inventory = backing
    }

    fun openFor(player: Player) {
        CoroutineScope(bukkitDispatcher).launch { open(player) }
    }

    suspend fun open(player: Player) {
        val uuid = player.uniqueId.toString()

        val saved = try {
            withContext(ioDispatcher) { ShulkerInventoryManager.getInventory(uuid) }
                .filter { it.slot in 0 until SLOTS }
        } catch (ex: Exception) {
            OriginsReforged.instance.logger.warning(
                "Failed to load shulker inventory for ${player.name}: ${ex.message}"
            )
            return
        }

        val holder = ShulkerInventoryHolder()
        val inventory = Bukkit.createInventory(holder, SLOTS, Component.text("Shulker Inventory"))
        holder.backing = inventory

        for (item in saved) {
            inventory.setItem(item.slot, item.itemStack)
        }

        player.openInventory(inventory)
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        if (event.inventory.holder !is ShulkerInventoryHolder) return
        val uuid = event.player.uniqueId.toString()
        val snapshot = (0 until SLOTS).map { slot ->
            ShulkerInventoryManager.SlotItem(slot, event.inventory.getItem(slot))
        }
        CoroutineScope(ioDispatcher).launch {
            ShulkerInventoryManager.saveInventory(uuid, snapshot)
        }
    }
}
