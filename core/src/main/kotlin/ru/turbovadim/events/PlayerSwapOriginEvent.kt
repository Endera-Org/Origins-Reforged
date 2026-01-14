package ru.turbovadim.events

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Minimal stub for PlayerSwapOriginEvent.
 * Full v2 implementation to be done later.
 */
class PlayerSwapOriginEvent(
    val player: Player,
    val reason: SwapReason
) : Event() {

    enum class SwapReason {
        COMMAND,
        ORB,
        CHOOSE,
        PLUGIN
    }

    companion object {
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlers
        }
    }

    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }
}
