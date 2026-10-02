package ru.turbovadim.v2.util

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.endera.enderalib.utils.async.EntityScheduler
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Combines hide reasons so removing one ability does not undo another. */
object PlayerVisibility {
    private data class PairKey(val viewer: UUID, val target: UUID)
    private val reasons = ConcurrentHashMap<PairKey, MutableSet<Key>>()

    fun setHidden(viewer: Player, target: Player, plugin: Plugin, reason: Key, hidden: () -> Boolean) {
        EntityScheduler.execute(plugin, viewer, {
            if (!viewer.isOnline) return@execute
            val pair = PairKey(viewer.uniqueId, target.uniqueId)
            val active = reasons.computeIfAbsent(pair) { ConcurrentHashMap.newKeySet() }
            val wasHidden = active.isNotEmpty()
            if (hidden()) active.add(reason) else active.remove(reason)
            if (active.isEmpty()) {
                reasons.remove(pair, active)
                if (wasHidden || !viewer.canSee(target)) viewer.showPlayer(plugin, target)
            } else if (!wasHidden) viewer.hidePlayer(plugin, target)
        })
    }

    fun forget(playerId: UUID) {
        reasons.keys.removeIf { it.viewer == playerId || it.target == playerId }
    }
}
