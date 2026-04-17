package ru.turbovadim.v2.ui

import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.view.ChestInterfaceView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.OriginsReforged
import ru.turbovadim.OriginsReforged.Companion.bukkitDispatcher
import ru.turbovadim.database.ShulkerInventoryManager
import ru.turbovadim.database.schema.ShulkerItem

object ShulkerInventoryUI {

    private const val ROWS = 1
    private const val SLOTS = 9
    private val ALL_REASONS = InventoryCloseEvent.Reason.entries

    fun openFor(player: Player) {
        CoroutineScope(bukkitDispatcher).launch { open(player) }
    }

    suspend fun open(player: Player) {
        val uuid = player.uniqueId.toString()

        val saved: List<ShulkerItem> = try {
            withContext(ioDispatcher) { ShulkerInventoryManager.getInventory(uuid) }
                .filter { it.slot in 0 until SLOTS }
        } catch (ex: Exception) {
            OriginsReforged.instance.logger.warning(
                "Failed to load shulker inventory for ${player.name}: ${ex.message}"
            )
            return
        }

        buildChestInterface {
            rows = ROWS
            preventClickingEmptySlots = false
            callCloseHandlerOnViewSwitch = true
            titleSupplier = { Component.text("Shulker Inventory") }

            withTransform { pane, _ ->
                for (item in saved) {
                    pane[0, item.slot] = StaticElement(drawable(item.itemStack), ClickHandler.ALLOW)
                }
            }

            addCloseHandler(ALL_REASONS) { _, view ->
                val inv = (view as ChestInterfaceView).inventory
                val items = (0 until SLOTS).map { slot ->
                    ShulkerInventoryManager.SlotItem(slot, inv.getItem(slot))
                }
                withContext(ioDispatcher) {
                    ShulkerInventoryManager.saveInventory(uuid, items)
                }
            }
        }.open(player)
    }
}
