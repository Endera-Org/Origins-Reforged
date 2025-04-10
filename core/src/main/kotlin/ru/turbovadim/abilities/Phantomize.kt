package ru.turbovadim.abilities

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerEvent
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.abilities.types.DependencyAbility
import ru.turbovadim.events.PlayerLeftClickEvent
import java.util.*

class Phantomize : DependencyAbility, Listener {

    private val phantomizedPlayers: MutableMap<UUID, Boolean> = HashMap()

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        CoroutineScope(ioDispatcher).launch {
            for (player in Bukkit.getOnlinePlayers().toList()) {
                if (isEnabled(player) && player.foodLevel <= 6) {
                    phantomizedPlayers[player.uniqueId] = false
                    val asyncPhantomizeToggleEvent = AsyncPhantomizeToggleEvent(player, false)
                    asyncPhantomizeToggleEvent.callEvent()
                }
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
        CoroutineScope(ioDispatcher).launch {
            if (event.hasBlock()) return@launch
            if (event.player.foodLevel <= 6) return@launch
            if (event.player.inventory.itemInMainHand.type != Material.AIR) return@launch

            runForAbilityAsync(event.player) { player ->
                val enabling = !phantomizedPlayers.getOrDefault(player.uniqueId, false)
                phantomizedPlayers[player.uniqueId] = enabling
                val asyncPhantomizeToggleEvent = AsyncPhantomizeToggleEvent(player, enabling)
                asyncPhantomizeToggleEvent.callEvent()
            }
        }
    }

    class AsyncPhantomizeToggleEvent(who: Player, private val enabling: Boolean) : PlayerEvent(who, true) {
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
