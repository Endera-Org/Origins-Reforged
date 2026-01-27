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
     * Process an origin change through the internal pipeline and external hooks.
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

        // External hook via Bukkit event (cancellable + editable), fired synchronously
        val changingEvent = PlayerOriginChangingEvent(
            player = request.player,
            layer = request.layer,
            oldOrigin = request.oldOrigin,
            newOrigin = request.newOrigin,
            reason = request.reason
        )
        Bukkit.getPluginManager().callEvent(changingEvent)

        request.layer = changingEvent.layer
        request.newOrigin = changingEvent.newOrigin
        request.cancelled = request.cancelled || changingEvent.isCancelled

        if (request.cancelled) {
            return OriginChangeResult(
                cancelled = true,
                layer = request.layer,
                oldOrigin = request.oldOrigin,
                newOrigin = request.newOrigin,
                changed = false
            )
        }

        val finalLayer = request.layer
        val finalOldOrigin = request.oldOrigin
        val finalNewOrigin = request.newOrigin

        // Apply the change to state on the main thread
        if (finalNewOrigin == null) {
            state.removeOrigin(finalLayer)
        } else {
            state.setOrigin(finalLayer, finalNewOrigin)
        }

        val changed = finalOldOrigin != finalNewOrigin || finalLayer != layer

        // Notify processors and fire post event
        notifyOriginChanged(player, finalLayer, finalOldOrigin, finalNewOrigin)

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
    private fun notifyOriginChanged(player: Player, layer: String, oldOrigin: Origin?, newOrigin: Origin?) {
        val state = container.playerStateManager.getState(player)

        // Apply passive effects on main thread
        scope.launch(container.dispatchers.main) {
            container.passiveEffectProcessor.applyPassiveEffects(player, state)
        }

        // Update periodic tasks
        container.periodicAbilityProcessor.updatePlayer(player.uniqueId, state.getAbilityKeys())

        // Fire Bukkit post event for other plugins to listen (synchronously)
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

/**
 * Bukkit pre-event fired before a player's origin changes.
 *
 * This event is synchronous and supports cancellation + mutation for addon hooks.
 */
class PlayerOriginChangingEvent(
    val player: Player,
    var layer: String,
    val oldOrigin: Origin?,
    var newOrigin: Origin?,
    val reason: OriginChangeReason
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
