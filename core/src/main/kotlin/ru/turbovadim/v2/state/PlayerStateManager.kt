package ru.turbovadim.v2.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.cancel
import org.endera.enderalib.utils.async.coroutines
import org.endera.enderalib.utils.async.withScheduler
import ru.turbovadim.v2.event.OriginChangeRequest
import ru.turbovadim.v2.event.OriginChangeResult
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.turbovadim.database.DatabaseManager
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.event.OriginChangeReason
import ru.turbovadim.v2.origin.Origin
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages PlayerOriginState instances for all online players.
 *
 * Responsibilities:
 * - Create/retrieve PlayerOriginState for players
 * - Clean up state on player disconnect (prevents memory leaks)
 * - Coordinate origin changes with the event bus
 */
class PlayerStateManager(
    private val container: OriginsContainer
) : Listener {

    private val states = ConcurrentHashMap<UUID, PlayerOriginState>()
    private val saveScope = CoroutineScope(container.dispatchers.io + SupervisorJob())
    private val saves = mutableMapOf<UUID, Job>()
    private val loads = ConcurrentHashMap<UUID, Job>()

    /**
     * Get or create state for a player.
     */
    fun getState(player: Player): PlayerOriginState {
        return getState(player.uniqueId)
    }

    /**
     * Get or create state for a player by UUID.
     */
    fun getState(playerId: UUID): PlayerOriginState {
        return states.getOrPut(playerId) {
            PlayerOriginState(playerId, container)
        }
    }

    /**
     * Get state for a player if it exists.
     */
    fun getStateOrNull(player: Player): PlayerOriginState? {
        return states[player.uniqueId]
    }

    /**
     * Get state for a player by UUID if it exists.
     */
    fun getStateOrNull(playerId: UUID): PlayerOriginState? {
        return states[playerId]
    }

    /**
     * Set a player's origin for a layer.
     * This triggers passive effect reapplication, event bus notification, and database persistence.
     */
    fun setOrigin(
        player: Player,
        layer: String,
        origin: Origin,
        reason: OriginChangeReason = OriginChangeReason.PLUGIN,
        beforeCommit: (OriginChangeRequest) -> Boolean = { true }
    ): OriginChangeResult {
        val result = container.eventBus.processOriginChangeSync(player, layer, origin, reason, beforeCommit)
        val newOrigin = result.newOrigin
        if (result.cancelled || newOrigin == null) {
            return result
        }

        persist(player.uniqueId, result.layer, newOrigin.key.asString())
        return result
    }

    /**
     * Remove a player's origin from a layer.
     * This triggers event bus notification and database persistence.
     */
    fun removeOrigin(
        player: Player,
        layer: String,
        reason: OriginChangeReason = OriginChangeReason.PLUGIN
    ): Origin? {
        val oldOrigin = getState(player).getOrigin(layer) ?: return null

        val result = container.eventBus.processOriginChangeSync(player, layer, null, reason)
        if (result.cancelled) {
            return oldOrigin
        }

        persist(player.uniqueId, result.layer, null)
        return oldOrigin
    }

    /** Serialize writes per player so an older change cannot overwrite a newer one. */
    private fun persist(playerId: UUID, layer: String, origin: String?) {
        synchronized(saves) {
            val previous = saves[playerId]
            val job = saveScope.launch {
                previous?.join()
                try {
                    DatabaseManager.updateOrigin(playerId.toString(), layer, origin)
                } catch (ex: Exception) {
                    container.plugin.logger.severe("Failed to persist origin for $playerId in $layer: ${ex.message}")
                }
            }
            saves[playerId] = job
            job.invokeOnCompletion { synchronized(saves) { saves.remove(playerId, job) } }
        }
    }

    /**
     * Called when a player quits - cleans up ALL state.
     */
    internal fun onPlayerQuit(player: Player) {
        loads.remove(player.uniqueId)?.cancel()
        getStateOrNull(player)?.origins?.values?.forEach {
            container.eventBus.triggerRemovedAbilityLifecycles(player, it, null)
        }
        container.attributeAbilityProcessor.removePlayer(player.uniqueId)
        container.nmsInvoker.removePlayer(player)
        ru.turbovadim.v2.util.PlayerVisibility.forget(player.uniqueId)
        container.cooldownManager.removePlayer(player.uniqueId)
        container.passiveEffectProcessor.removePassiveEffects(player)
        container.passiveEffectProcessor.removePlayer(player.uniqueId)
        val state = states.remove(player.uniqueId)
        state?.clear()

        // Notify periodic processor to remove player's tasks
        container.periodicAbilityProcessor.removePlayer(player.uniqueId)
    }

    /**
     * Clear all states. Called on plugin disable.
     */
    internal fun clearAll() {
        loads.values.forEach { it.cancel() }
        loads.clear()
        runBlocking {
            synchronized(saves) { saves.values.toList() }.forEach { it.join() }
        }
        saveScope.cancel()
        states.values.forEach { it.clear() }
        states.clear()
    }

    /**
     * Get all currently tracked player IDs.
     */
    fun getTrackedPlayers(): Set<UUID> = states.keys.toSet()

    /**
     * Get the count of tracked players.
     */
    fun getTrackedPlayerCount(): Int = states.size

    // Bukkit event handlers

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        // Pre-create state for the player
        val session = getState(player)

        // Load origins from database asynchronously
        loads[player.uniqueId] = container.plugin.coroutines.launchIo {
            loadOriginsFromDatabase(player, session)
        }
    }

    /**
     * Load player's origins from the database and apply them.
     */
    private suspend fun loadOriginsFromDatabase(player: Player, session: PlayerOriginState) {
        try {
            val savedOrigins = try {
                synchronized(saves) { saves[player.uniqueId] }?.join()
                DatabaseManager.getSelectedOrigins(player.uniqueId.toString())
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                container.plugin.logger.severe(
                    "Failed to load saved origins for ${player.name}: ${t.message}"
                )
                t.printStackTrace()
                session.dbLoadFailed = true
                return
            }

            if (savedOrigins != null) {
                // Resolve each layer-origin pair and apply
                for ((layer, originName) in savedOrigins.layerOriginPairs) {
                    if (originName == null) continue

                    val origin = container.originRegistry.getByName(originName)
                    if (origin == null) {
                        session.dbLoadFailed = true
                        container.plugin.logger.warning(
                            "Could not find origin '$originName' for player ${player.name} (layer: $layer)"
                        )
                        continue
                    }

                    // Apply on the player's owning thread without re-saving to the database
                    player.withScheduler(container.plugin) {
                        if (player.isOnline && getStateOrNull(player) === session) {
                            val result = container.eventBus.processOriginChangeSync(player, layer, origin, OriginChangeReason.DATABASE_LOAD)
                            if (result.cancelled) session.dbLoadFailed = true
                        }
                    }
                }
            }
        } finally {
            // Mark load complete so join-flow listeners can proceed.
            if (getStateOrNull(player.uniqueId) === session) session.dbLoadComplete = true
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        onPlayerQuit(event.player)
    }

    /**
     * Register this manager as a Bukkit listener.
     */
    fun registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, container.plugin)
    }
}
