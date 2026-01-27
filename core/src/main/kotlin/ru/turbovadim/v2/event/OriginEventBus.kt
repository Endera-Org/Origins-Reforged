package ru.turbovadim.v2.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerRespawnEvent
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.origin.Origin
import java.util.concurrent.CopyOnWriteArrayList

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
    private val interceptors = CopyOnWriteArrayList<OriginChangeInterceptor>()
    private val changedListeners = CopyOnWriteArrayList<OriginChangedListener>()

    /**
     * Register an internal origin change interceptor.
     *
     * Interceptors run on the main thread and may cancel or rewrite the request.
     */
    fun registerInterceptor(interceptor: OriginChangeInterceptor) {
        interceptors.addIfAbsent(interceptor)
    }

    /**
     * Unregister an internal origin change interceptor.
     */
    fun unregisterInterceptor(interceptor: OriginChangeInterceptor) {
        interceptors.remove(interceptor)
    }

    /**
     * Register an internal post-change listener.
     */
    fun registerChangedListener(listener: OriginChangedListener) {
        changedListeners.addIfAbsent(listener)
    }

    /**
     * Unregister an internal post-change listener.
     */
    fun unregisterChangedListener(listener: OriginChangedListener) {
        changedListeners.remove(listener)
    }

    /**
     * Process an origin change through the internal pipeline.
     *
     * This function always executes on the main thread, even if called from async code.
     */
    suspend fun processOriginChange(
        player: Player,
        layer: String,
        newOrigin: Origin?,
        reason: OriginChangeReason
    ): OriginChangeResult = withContext(container.dispatchers.main) {
        processOriginChangeSync(player, layer, newOrigin, reason)
    }

    /**
     * Process an origin change on the main thread.
     *
     * Call this only from synchronous/main-thread code paths.
     */
    fun processOriginChangeSync(
        player: Player,
        layer: String,
        newOrigin: Origin?,
        reason: OriginChangeReason
    ): OriginChangeResult {
        check(Bukkit.isPrimaryThread()) {
            "Origin changes must be processed on the main thread."
        }
        val state = container.playerStateManager.getState(player)
        val oldOrigin = state.getOrigin(layer)

        val request = OriginChangeRequest(
            player = player,
            layer = layer,
            oldOrigin = oldOrigin,
            newOrigin = newOrigin,
            reason = reason
        )

        // Internal interceptors (cancellable + editable)
        for (interceptor in interceptors) {
            interceptor.intercept(request)
            if (request.cancelled) {
                return OriginChangeResult(
                    cancelled = true,
                    layer = request.layer,
                    oldOrigin = request.oldOrigin,
                    newOrigin = request.newOrigin,
                    changed = false
                )
            }
        }

        val finalLayer = request.layer
        val finalNewOrigin = request.newOrigin
        val finalOldOrigin = if (finalLayer == layer) {
            request.oldOrigin
        } else {
            state.getOrigin(finalLayer)
        }

        // Apply the change to state on the main thread
        if (finalNewOrigin == null) {
            state.removeOrigin(finalLayer)
        } else {
            state.setOrigin(finalLayer, finalNewOrigin)
        }

        val changed = finalOldOrigin != finalNewOrigin || finalLayer != layer

        // Notify processors and fire internal post-change listeners
        notifyOriginChanged(player, finalLayer, finalOldOrigin, finalNewOrigin, reason)

        return OriginChangeResult(
            cancelled = false,
            layer = finalLayer,
            oldOrigin = finalOldOrigin,
            newOrigin = finalNewOrigin,
            changed = changed
        )
    }

    /**
     * Called after a player's origin has been applied to state.
     * Triggers passive effect reapplication, periodic task scheduling, and post hooks.
     */
    private fun notifyOriginChanged(
        player: Player,
        layer: String,
        oldOrigin: Origin?,
        newOrigin: Origin?,
        reason: OriginChangeReason
    ) {
        val state = container.playerStateManager.getState(player)

        // Apply passive effects on main thread
        scope.launch(container.dispatchers.main) {
            container.passiveEffectProcessor.applyPassiveEffects(player, state)
        }

        // Update periodic tasks
        container.periodicAbilityProcessor.updatePlayer(player.uniqueId, state.getAbilityKeys())

        // Fire internal post-change listeners
        if (changedListeners.isEmpty()) return

        val event = OriginChangedEvent(player, layer, oldOrigin, newOrigin, reason)
        for (listener in changedListeners) {
            try {
                listener.onChanged(event)
            } catch (t: Throwable) {
                container.plugin.logger.severe(
                    "OriginChangedListener failed for ${player.name} (layer: $layer): ${t.message}"
                )
                t.printStackTrace()
            }
        }
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
        // Check if one world is disabled and the other isn't
        val disabledWorlds = try {
            OriginsReforged.mainConfig.worlds.disabledWorlds
        } catch (e: Exception) {
            return false // Config not initialized, assume same rules
        }

        val fromDisabled = from.name in disabledWorlds
        val toDisabled = to.name in disabledWorlds

        // Rules differ if one is disabled and the other isn't
        return fromDisabled != toDisabled
    }
}
