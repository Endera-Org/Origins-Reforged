package ru.turbovadim.events

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import ru.turbovadim.OriginsReforged.Companion.instance

@Suppress("unused")
class PlayerLeftClickEvent(private val playerInteractEvent: PlayerInteractEvent) : PlayerEvent(
    playerInteractEvent.getPlayer()
) {
    val interactionPoint: Location?
        get() = playerInteractEvent.interactionPoint

    fun hasBlock(): Boolean {
        return playerInteractEvent.hasBlock()
    }

    fun hasItem(): Boolean {
        return playerInteractEvent.hasItem()
    }

    val item: ItemStack?
        get() = playerInteractEvent.getItem()

    val material: Material
        get() = playerInteractEvent.getMaterial()

    val clickedBlock: Block?
        get() = playerInteractEvent.clickedBlock

    val blockFace: BlockFace?
        get() = playerInteractEvent.getBlockFace()

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    class PlayerLeftClickEventListener : Listener {
        var lastInteractionTickMap: MutableMap<Player?, Int?> = HashMap<Player?, Int?>()

        @EventHandler
        fun onPlayerInteract(event: PlayerInteractEvent) {
            if (!event.getAction().isLeftClick) {
                return
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(instance, Runnable {
                if (lastInteractionTickMap.getOrDefault(
                        event.getPlayer(),
                        -1
                    )!! >= Bukkit.getCurrentTick()
                ) return@Runnable
                lastInteractionTickMap.put(event.getPlayer(), Bukkit.getCurrentTick())
                PlayerLeftClickEvent(event).callEvent()
            })
        }

        @EventHandler
        fun onPlayerDropItem(event: PlayerDropItemEvent) {
            lastInteractionTickMap.put(event.getPlayer(), Bukkit.getCurrentTick() + 1)
        }

        @EventHandler
        fun onBlockBreak(event: BlockBreakEvent) {
            lastInteractionTickMap.put(event.player, Bukkit.getCurrentTick() + 1)
        }
    }

    companion object {
        private val handlerList: HandlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlerList
    }

}
