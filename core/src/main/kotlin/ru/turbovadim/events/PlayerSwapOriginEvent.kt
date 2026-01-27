package ru.turbovadim.events

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import ru.turbovadim.v2.origin.Origin

/**
 * Event fired when a player's origin is about to change.
 *
 * This event is cancellable - cancelling it will prevent the origin swap.
 *
 * @property player The player whose origin is changing
 * @property layer The layer being changed (e.g., "origin", "class")
 * @property oldOrigin The player's current origin (null if none)
 * @property newOrigin The origin the player is swapping to (null if being cleared)
 * @property reason The reason for the origin swap
 */
class PlayerSwapOriginEvent(
    val player: Player,
    val layer: String,
    val oldOrigin: Origin?,
    val newOrigin: Origin?,
    val reason: SwapReason
) : Event(), Cancellable {

    private var cancelled = false

    /**
     * Reasons for an origin swap.
     */
    enum class SwapReason {
        /** Swapped via /origin set command */
        COMMAND,
        /** Swapped using an Orb of Origin item */
        ORB,
        /** Player selected during initial choice */
        CHOOSE,
        /** Swapped programmatically by a plugin */
        PLUGIN,
        /** Swapped via randomization */
        RANDOM,
        /** Admin forced the swap */
        ADMIN
    }

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    /**
     * Check if this is a new player selecting their first origin.
     */
    fun isFirstSelection(): Boolean = oldOrigin == null && reason == SwapReason.CHOOSE

    /**
     * Check if the origin is being cleared (set to null/human).
     */
    fun isClearing(): Boolean = newOrigin == null

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
