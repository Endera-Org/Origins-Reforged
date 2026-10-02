package ru.turbovadim.v2.event

import org.endera.enderalib.utils.async.withScheduler
import org.endera.enderalib.isFolia
import net.kyori.adventure.text.Component
import ru.turbovadim.database.DatabaseManager
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.endera.enderalib.utils.async.runTask
import org.endera.enderalib.utils.async.runTaskLater
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

    private val changeLock = Any()
    private val interceptors = CopyOnWriteArrayList<OriginChangeInterceptor>()
    private val changedListeners = CopyOnWriteArrayList<OriginChangedListener>()

    /**
     * Register an internal origin change interceptor.
     *
     * Interceptors run on the player's thread and may cancel or rewrite the request.
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
     * Executes on the player's owning thread, including calls from async code.
     */
    suspend fun processOriginChange(
        player: Player,
        layer: String,
        newOrigin: Origin?,
        reason: OriginChangeReason
    ): OriginChangeResult = player.withScheduler(container.plugin) {
        processOriginChangeSync(player, layer, newOrigin, reason)
    }

    /**
     * Process an origin change on the player's owning thread.
     *
     * Call only from the player's owning thread.
     * [beforeCommit] runs after interception, before state changes, and may reject the change.
     */
    fun processOriginChangeSync(
        player: Player,
        layer: String,
        newOrigin: Origin?,
        reason: OriginChangeReason,
        beforeCommit: (OriginChangeRequest) -> Boolean = { true }
    ): OriginChangeResult = synchronized(changeLock) {
        check(if (isFolia) Bukkit.isOwnedByCurrentRegion(player) else Bukkit.isPrimaryThread()) {
            "Origin changes must be processed on the player's thread."
        }
        val state = container.playerStateManager.getState(player)
        val userChange = reason == OriginChangeReason.UI || reason == OriginChangeReason.ORB ||
            reason == OriginChangeReason.COMMAND
        if ((reason != OriginChangeReason.DATABASE_LOAD && !state.dbLoadComplete) ||
            (userChange && state.dbLoadFailed)) {
            player.sendMessage(Component.text("Your saved origins are not loaded yet."))
            return OriginChangeResult(true, layer, state.getOrigin(layer), newOrigin, false)
        }
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

        if (!beforeCommit(request)) {
            return OriginChangeResult(true, request.layer, request.oldOrigin, request.newOrigin, false)
        }
        val finalLayer = request.layer
        val finalNewOrigin = request.newOrigin
        val finalOldOrigin = if (finalLayer == layer) {
            request.oldOrigin
        } else {
            state.getOrigin(finalLayer)
        }

        // Apply only after restrictions and pre-commit work succeed.
        if (finalNewOrigin == null) {
            state.removeOrigin(finalLayer)
        } else {
            state.setOrigin(finalLayer, finalNewOrigin)
            if (reason != OriginChangeReason.DATABASE_LOAD) {
                DatabaseManager.recordUsedOriginSync(player.uniqueId.toString(), finalNewOrigin.key.asString())
            }
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
     *
     * Called on the player's owning thread by the change pipeline.
     */
    private fun notifyOriginChanged(
        player: Player,
        layer: String,
        oldOrigin: Origin?,
        newOrigin: Origin?,
        reason: OriginChangeReason
    ) {
        val state = container.playerStateManager.getState(player)

        // Register periodic work before applying the new effects.
        container.periodicAbilityProcessor.updatePlayer(player.uniqueId, state.getAbilityKeys())

        // The change pipeline already runs on the player's owning thread.
        triggerRemovedAbilityLifecycles(player, oldOrigin, newOrigin)
        container.passiveEffectProcessor.applyPassiveEffects(player, state)
        container.attributeAbilityProcessor.updatePlayer(player.uniqueId, state.getAbilityKeys())

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
     * Trigger onDependencyDisabled lifecycle callbacks for abilities being removed.
     */
    internal fun triggerRemovedAbilityLifecycles(
        player: Player,
        oldOrigin: Origin?,
        newOrigin: Origin?
    ) {
        if (oldOrigin == null) return

        val oldAbilities = oldOrigin.abilityKeys
        val newAbilities = newOrigin?.abilityKeys ?: emptySet()

        // Find abilities being removed
        val removedAbilities = oldAbilities - newAbilities

        for (abilityKey in removedAbilities) {
            val ability = container.abilityRegistry.get(abilityKey) ?: continue

            // Get config accessor for the ability
            val config = container.configLoader.getAccessor(abilityKey, ability.defaultOptions)

            // Trigger onDependencyDisabled for this ability (treating removal as "disabled")
            for (effect in ability.effects) {
                if (effect is ru.turbovadim.v2.ability.AbilityEffect.Lifecycle.OnDependencyDisabled) {
                    try {
                        effect.handler.onStateChange(player, config)
                    } catch (t: Throwable) {
                        container.plugin.logger.warning(
                            "Lifecycle callback failed for ability $abilityKey: ${t.message}"
                        )
                    }
                }
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

            // Folia: reapply passives on the player's new region thread.
            player.runTask(container.plugin) {
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

        // Delay slightly to ensure player is fully respawned (entity-tied).
        // We're already on the player's region thread inside runTaskLater,
        // so apply passives directly — no need to hop through dispatchers.main.
        player.runTaskLater(container.plugin, 1L) {
            val state = container.playerStateManager.getState(player)
            container.passiveEffectProcessor.applyPassiveEffects(player, state)
        }
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
