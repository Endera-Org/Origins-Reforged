package ru.turbovadim.packetsenders

import org.bukkit.entity.Entity
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.EntityEvent

@Suppress("unused")
class FantasyEntityDismountEvent(
    what: Entity,
    val dismounted: Entity,
    private val isCancellable: Boolean = true
) : EntityEvent(what), Cancellable {

    constructor(what: Entity, dismounted: Entity) : this(what, dismounted, true)

    private var cancelled = false

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled(cancel: Boolean) {
        if (cancel && !isCancellable) {
            return
        }
        this.cancelled = cancel
    }

    fun isCancellable(): Boolean {
        return isCancellable
    }

    companion object {
        private val handlerList: HandlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlerList
    }
}