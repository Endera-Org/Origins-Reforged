package ru.turbovadim.abilities

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerEvent
import ru.turbovadim.abilities.types.DependencyAbility
import ru.turbovadim.events.PlayerLeftClickEvent
import java.util.*

class Phantomize : DependencyAbility, Listener {

    private val phantomizedPlayers: MutableMap<UUID, Boolean> = HashMap()

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        for (player in Bukkit.getOnlinePlayers()) {
            if (isEnabled(player) && player.foodLevel <= 6) {
                phantomizedPlayers[player.uniqueId] = false
                val phantomizeToggleEvent = PhantomizeToggleEvent(player, false)
                phantomizeToggleEvent.callEvent()
            }
        }
    }

    override fun getKey(): Key {
        return Key.key("origins:phantomize")
    }

    override fun isEnabled(player: Player): Boolean {
        return phantomizedPlayers.getOrDefault(player.uniqueId, false)
    }

    @EventHandler
    fun onLeftClick(event: PlayerLeftClickEvent) {
        if (event.hasBlock()) return
        if (event.player.foodLevel <= 6) return
        if (event.player.inventory.itemInMainHand.type != Material.AIR) return

        runForAbility(event.player) { player ->
            val enabling = !phantomizedPlayers.getOrDefault(player.uniqueId, false)
            phantomizedPlayers[player.uniqueId] = enabling
            val phantomizeToggleEvent = PhantomizeToggleEvent(player, enabling)
            phantomizeToggleEvent.callEvent()
        }
    }

    class PhantomizeToggleEvent(who: Player, private val enabling: Boolean) : PlayerEvent(who) {
        fun isEnabling(): Boolean = enabling

        override fun getHandlers(): HandlerList {
            return HANDLERS
        }

        companion object {
            private val HANDLERS = HandlerList()

            @JvmStatic
            fun getHandlerList(): HandlerList = HANDLERS
        }
    }
}
