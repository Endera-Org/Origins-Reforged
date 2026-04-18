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
import org.endera.enderalib.utils.async.runTask
import ru.turbovadim.OriginsReforged.Companion.instance

@Suppress("unused")
class PlayerLeftClickEvent(private val playerInteractEvent: PlayerInteractEvent) : PlayerEvent(
    playerInteractEvent.getPlayer()
) {
    val interactionPoint: Location? = playerInteractEvent.interactionPoint

    fun hasBlock(): Boolean {
        return playerInteractEvent.hasBlock()
    }

    fun hasItem(): Boolean {
        return playerInteractEvent.hasItem()
    }

    val item: ItemStack? = playerInteractEvent.getItem()

    val material: Material = playerInteractEvent.getMaterial()

    val clickedBlock: Block? = playerInteractEvent.clickedBlock

    val blockFace: BlockFace? = playerInteractEvent.getBlockFace()

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
            // Entity-tied: the rescheduled work dispatches an event about this player.
            event.player.runTask(instance) {
                if (lastInteractionTickMap.getOrDefault(
                        event.getPlayer(),
                        -1
                    )!! >= Bukkit.getCurrentTick()
                ) return@runTask
                lastInteractionTickMap.put(event.getPlayer(), Bukkit.getCurrentTick())
                PlayerLeftClickEvent(event).callEvent()
            }
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
