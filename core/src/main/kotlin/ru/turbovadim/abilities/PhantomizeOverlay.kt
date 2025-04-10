package ru.turbovadim.abilities

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWorldBorderWarningReach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.abilities.Phantomize.AsyncPhantomizeToggleEvent
import ru.turbovadim.abilities.types.DependantAbility

class PhantomizeOverlay : DependantAbility, Listener {

    override fun getKey(): Key {
        return Key.key("origins:phantomize_overlay")
    }

    override val dependencyKey: Key = Key.key("origins:phantomize")

    @EventHandler
    fun onAsyncPhantomizeToggle(event: AsyncPhantomizeToggleEvent) {
        updatePhantomizeOverlay(event.player)
    }

    @EventHandler
    fun onPlayerPostRespawn(event: PlayerPostRespawnEvent) {
        CoroutineScope(ioDispatcher).launch {
            updatePhantomizeOverlay(event.player)
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        CoroutineScope(ioDispatcher).launch {
            updatePhantomizeOverlay(event.player)
        }
    }

    private fun updatePhantomizeOverlay(player: Player) {
            val packet = if (dependency.isEnabled(player)) {
                WrapperPlayServerWorldBorderWarningReach(
                    player.world.worldBorder.size.times(2).toInt()
                )
            } else {
                WrapperPlayServerWorldBorderWarningReach(
                    player.world.worldBorder.warningDistance
                )
            }
            PacketEvents.getAPI().playerManager.sendPacket(player, packet)
//        NMSInvoker.setWorldBorderOverlay(player, dependency.isEnabled(player))
    }
}
