package ru.turbovadim.v2.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
import java.util.UUID
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
        reason: OriginChangeReason = OriginChangeReason.PLUGIN
    ) {
        val result = container.eventBus.processOriginChangeSync(player, layer, origin, reason)
        if (result.cancelled || result.newOrigin == null) {
            return
        }

        // Persist to database asynchronously using the final, possibly rewritten layer/origin
        CoroutineScope(container.dispatchers.io).launch {
            try {
                DatabaseManager.updateOrigin(
                    player.uniqueId.toString(),
                    result.layer,
                    result.newOrigin.name
                )
            } catch (t: Throwable) {
                container.plugin.logger.severe(
                    "Failed to persist origin '${result.newOrigin.name}' for ${player.name} " +
                        "(layer: ${result.layer}): ${t.message}"
                )
                t.printStackTrace()
            }
        }
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

        // Persist removal to database asynchronously using the final layer
        CoroutineScope(container.dispatchers.io).launch {
            try {
                DatabaseManager.updateOrigin(player.uniqueId.toString(), result.layer, null)
            } catch (t: Throwable) {
                container.plugin.logger.severe(
                    "Failed to persist origin removal for ${player.name} (layer: ${result.layer}): ${t.message}"
                )
                t.printStackTrace()
            }
        }

        return oldOrigin
    }

    /**
     * Called when a player quits - cleans up ALL state.
     */
    internal fun onPlayerQuit(player: Player) {
        val state = states.remove(player.uniqueId)
        state?.clear()

        // Notify periodic processor to remove player's tasks
        container.periodicAbilityProcessor.removePlayer(player.uniqueId)
    }

    /**
     * Clear all states. Called on plugin disable.
     */
    internal fun clearAll() {
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
        getState(player)

        // Load origins from database asynchronously
        CoroutineScope(container.dispatchers.io).launch {
            loadOriginsFromDatabase(player)
        }
    }

    /**
     * Load player's origins from the database and apply them.
     */
    private suspend fun loadOriginsFromDatabase(player: Player) {
        val savedOrigins = try {
            DatabaseManager.getSelectedOrigins(player.uniqueId.toString())
        } catch (t: Throwable) {
            container.plugin.logger.severe(
                "Failed to load saved origins for ${player.name}: ${t.message}"
            )
            t.printStackTrace()
            null
        } ?: return // No saved origins (or load failed) for this player

        // Resolve each layer-origin pair and apply
        for ((layer, originName) in savedOrigins.layerOriginPairs) {
            if (originName == null) continue

            val origin = container.originRegistry.getByName(originName)
            if (origin == null) {
                container.plugin.logger.warning(
                    "Could not find origin '$originName' for player ${player.name} (layer: $layer)"
                )
                continue
            }

            // Apply via pipeline on the main thread without re-saving to database
            if (player.isOnline) {
                container.eventBus.processOriginChange(
                    player = player,
                    layer = layer,
                    newOrigin = origin,
                    reason = OriginChangeReason.DATABASE_LOAD
                )
            }
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
