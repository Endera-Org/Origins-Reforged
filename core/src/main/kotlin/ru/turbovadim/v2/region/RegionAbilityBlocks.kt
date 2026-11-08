package ru.turbovadim.v2.region

import net.kyori.adventure.key.Key
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Ability keys blocked for each player by the region they stand in; `*` blocks every ability. */
class RegionAbilityBlocks {
    private val blocked = ConcurrentHashMap<UUID, Set<String>>()

    fun isBlocked(playerId: UUID, key: Key): Boolean {
        val keys = blocked[playerId] ?: return false
        return "*" in keys || key.asString() in keys
    }

    /** Replaces the player's blocked keys and returns whether they changed. */
    fun update(playerId: UUID, keys: Set<String>): Boolean {
        val previous = if (keys.isEmpty()) blocked.remove(playerId) else blocked.put(playerId, keys)
        return (previous ?: emptySet()) != keys
    }

    fun remove(playerId: UUID) {
        blocked.remove(playerId)
    }
}
