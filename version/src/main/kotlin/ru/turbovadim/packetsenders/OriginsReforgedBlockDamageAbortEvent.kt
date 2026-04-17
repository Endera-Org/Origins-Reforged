package ru.turbovadim.packetsenders

import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.block.BlockEvent
import org.bukkit.inventory.ItemStack

@Suppress("unused")
class OriginsReforgedBlockDamageAbortEvent(
    val player: Player,
    block: Block,
    val itemInHand: ItemStack
) : BlockEvent(block) {

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
