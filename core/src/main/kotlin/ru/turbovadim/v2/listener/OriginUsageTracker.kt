package ru.turbovadim.v2.listener

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.turbovadim.database.DatabaseManager
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.event.OriginChangeReason
import ru.turbovadim.v2.event.OriginChangedEvent
import ru.turbovadim.v2.event.OriginChangedListener

/**
 * Records every origin a player acquires in the `used_origins` table.
 *
 * Fuels:
 * - `restrictions.reusingOrigins = PERPLAYER / ALL`
 * - `swapCommand.vault.permanentPurchases`
 *
 * Updates the in-memory cache synchronously and schedules the DB write
 * asynchronously.
 */
class OriginUsageTracker(
    private val container: OriginsContainer
) : OriginChangedListener {

    override fun onChanged(event: OriginChangedEvent) {
        // Don't record restored-from-DB origins as fresh usages.
        if (event.reason == OriginChangeReason.DATABASE_LOAD) return

        val origin = event.newOrigin ?: return
        val uuid = event.player.uniqueId.toString()

        // If the player already used this origin, skip the DB insert (cache is unique anyway).
        val alreadyRecorded = DatabaseManager.getUsedOriginsSync(uuid)
            .any { it.equals(origin.name, ignoreCase = true) }
        if (alreadyRecorded) return

        // Update cache right away so interceptors see the newest state.
        DatabaseManager.recordUsedOriginSync(uuid, origin.name)

        // Persist asynchronously.
        CoroutineScope(container.dispatchers.io).launch {
            try {
                DatabaseManager.addOriginToHistory(uuid, origin.name)
            } catch (t: Throwable) {
                container.plugin.logger.warning(
                    "Failed to persist used-origin history for ${event.player.name}: ${t.message}"
                )
            }
        }
    }
}
