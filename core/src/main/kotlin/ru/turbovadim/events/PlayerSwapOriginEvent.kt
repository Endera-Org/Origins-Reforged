package ru.turbovadim.events

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent
import ru.turbovadim.Origin

@Suppress("unused")
class PlayerSwapOriginEvent(
    who: Player,
    val reason: SwapReason?,
    var isResetPlayer: Boolean,
    val oldOrigin: Origin?,
    @JvmField var newOrigin: Origin?
) : PlayerEvent(who), Cancellable {
    private var cancelled = false

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    override fun isCancelled(): Boolean {
        return this.cancelled
    }

    override fun setCancelled(cancel: Boolean) {
        this.cancelled = cancel
    }

    enum class SwapReason(val reason: String) {
        /**
         * Swapped origin with the /origin swap command
         */
        COMMAND("command"),

        /**
         * Swapped origin using an Orb of Origin
         */
        ORB_OF_ORIGIN("orb"),

        /**
         * Swapped origin due to having died and respawned
         */
        DIED("died"),

        /**
         * Swapped origin due to not having one yet
         */
        INITIAL("initial"),

        /**
         * Swapped origin due to another plugin
         */
        PLUGIN("plugin"),

        /**
         * Unknown swap reason
         */
        UNKNOWN("unknown");

        companion object {
            fun get(reason: String?): SwapReason {
                for (swapReason in entries) {
                    if (swapReason.reason == reason) return swapReason
                }
                return UNKNOWN
            }
        }
    }

    companion object {
        private val handlerList: HandlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlerList
    }
}
