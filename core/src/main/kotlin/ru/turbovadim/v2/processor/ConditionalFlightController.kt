package ru.turbovadim.v2.processor

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import ru.turbovadim.v2.di.OriginsContainer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinates abilities that temporarily own a player's flight capability.
 *
 * The first owner captures the pre-existing flight state. That state is only
 * restored after the last owner releases it, so overlapping abilities cannot
 * disable or leak one another's flight.
 */
class ConditionalFlightController(private val container: OriginsContainer) : Listener {

    private data class FlightState(
        val previousAllowFlight: Boolean,
        val previousFlying: Boolean,
        val previousFlySpeed: Float,
        val owners: MutableSet<Key> = mutableSetOf()
    )

    private val states = ConcurrentHashMap<UUID, FlightState>()

    fun registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, container.plugin)
    }

    fun acquire(player: Player, owner: Key) {
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) return

        val state = states.computeIfAbsent(player.uniqueId) {
            FlightState(
                previousAllowFlight = player.allowFlight,
                previousFlying = player.isFlying,
                previousFlySpeed = player.flySpeed
            )
        }

        synchronized(state) {
            state.owners.add(owner)
        }
        player.allowFlight = true
    }

    fun release(player: Player, owner: Key) {
        val state = states[player.uniqueId] ?: return
        val shouldRestore = synchronized(state) {
            state.owners.remove(owner)
            state.owners.isEmpty()
        }
        if (!shouldRestore || !states.remove(player.uniqueId, state)) return

        restore(player, state)
    }

    fun reassert(player: Player) {
        val state = states[player.uniqueId] ?: return
        val hasOwners = synchronized(state) { state.owners.isNotEmpty() }
        if (hasOwners && player.gameMode != GameMode.CREATIVE && player.gameMode != GameMode.SPECTATOR) {
            player.allowFlight = true
        }
    }

    fun shutdown() {
        Bukkit.getOnlinePlayers().forEach { player ->
            states.remove(player.uniqueId)?.let { restore(player, it) }
        }
        states.clear()
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        states.remove(event.player.uniqueId)
    }

    private fun restore(player: Player, state: FlightState) {
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) return

        player.allowFlight = state.previousAllowFlight
        player.isFlying = state.previousAllowFlight && state.previousFlying
        player.flySpeed = state.previousFlySpeed.coerceIn(-1.0f, 1.0f)
    }
}
