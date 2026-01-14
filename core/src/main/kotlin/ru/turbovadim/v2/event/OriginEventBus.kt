package ru.turbovadim.v2.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerRespawnEvent
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.origin.Origin

/**
 * Central event bus for origin-related events.
 *
 * This replaces the polling-based `updateAllPlayers()` with event-driven updates.
 * Passive effects are applied only when:
 * - Origin changes
 * - World changes (if different rules apply)
 * - Player respawns
 */
class OriginEventBus(private val container: OriginsContainer) : Listener {

    private val scope = CoroutineScope(container.dispatchers.io + SupervisorJob())

    /**
     * Called when a player's origin changes.
     * Triggers passive effect reapplication and periodic task scheduling.
     */
    fun onOriginChanged(player: Player, layer: String, oldOrigin: Origin?, newOrigin: Origin?) {
        val state = container.playerStateManager.getState(player)

        // Apply passive effects on main thread
        scope.launch(container.dispatchers.main) {
            container.passiveEffectProcessor.applyPassiveEffects(player, state)
        }

        // Update periodic tasks
        container.periodicAbilityProcessor.updatePlayer(player.uniqueId, state.getAbilityKeys())

        // Fire Bukkit event for other plugins to listen
        val event = PlayerOriginChangedEvent(player, layer, oldOrigin, newOrigin)
        Bukkit.getPluginManager().callEvent(event)
    }

    /**
     * Called when a player changes worlds.
     * Only recalculates if the new world has different origin rules.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onWorldChanged(event: PlayerChangedWorldEvent) {
        val player = event.player
        val fromWorld = event.from
        val toWorld = player.world

        // Check if world rules differ
        if (worldRulesDiffer(fromWorld, toWorld)) {
            val state = container.playerStateManager.getState(player)
            state.invalidateCache()

            scope.launch(container.dispatchers.main) {
                container.passiveEffectProcessor.applyPassiveEffects(player, state)
            }
        }
    }

    /**
     * Called when a player respawns.
     * Reapplies passive effects.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player

        // Delay slightly to ensure player is fully respawned
        Bukkit.getScheduler().runTaskLater(container.plugin, Runnable {
            val state = container.playerStateManager.getState(player)

            scope.launch(container.dispatchers.main) {
                container.passiveEffectProcessor.applyPassiveEffects(player, state)
            }
        }, 1L)
    }

    /**
     * Register this event bus as a Bukkit listener.
     */
    fun registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, container.plugin)
    }

    // Private helpers

    private fun worldRulesDiffer(from: World, to: World): Boolean {
        // TODO: Check config for world-specific origin rules
        // For now, assume all worlds have the same rules
        return false
    }
}

/**
 * Bukkit event fired when a player's origin changes.
 */
class PlayerOriginChangedEvent(
    val player: Player,
    val layer: String,
    val oldOrigin: Origin?,
    val newOrigin: Origin?
) : org.bukkit.event.Event(), org.bukkit.event.Cancellable {

    private var cancelled = false

    override fun getHandlers(): org.bukkit.event.HandlerList = handlerList

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    companion object {
        @JvmStatic
        val handlerList = org.bukkit.event.HandlerList()
    }
}
