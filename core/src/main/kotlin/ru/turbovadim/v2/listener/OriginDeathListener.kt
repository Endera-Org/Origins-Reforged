package ru.turbovadim.v2.listener

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerRespawnEvent
import org.endera.enderalib.utils.async.runTaskLater
import ru.turbovadim.OriginsReforged.Companion.bukkitDispatcher
import ru.turbovadim.OriginsReforged.Companion.mainConfig
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.event.OriginChangeReason
import ru.turbovadim.v2.ui.OriginSelectorUI

/**
 * Implements `originSelection.deathOriginChange`:
 * when a player respawns, optionally let them pick a new origin.
 *
 * - If `originSelection.randomize[layer]` is true for a layer, re-roll a
 *   random origin for that layer.
 * - Otherwise, open the origin selection GUI for the first registered layer.
 */
class OriginDeathListener(
    private val container: OriginsContainer
) : Listener {

    private val plugin get() = container.plugin

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        if (!mainConfig.originSelection.deathOriginChange) return

        val player = event.player

        // Delay a couple of ticks so the respawn is fully complete (entity-tied).
        player.runTaskLater(plugin, 5L) {
            if (!player.isOnline) return@runTaskLater

            val layers = container.originLoader.layers
            if (layers.isEmpty()) return@runTaskLater

            var openedGui = false
            for (layer in layers) {
                if (container.originLoader.isRandomOnSelection(layer)) {
                    // Re-roll
                    val origin = container.originRegistry.getRandomOrigin(layer) ?: continue
                    container.playerStateManager.setOrigin(
                        player, layer, origin, OriginChangeReason.PLUGIN
                    )
                    continue
                }
                if (!openedGui) {
                    openedGui = true
                    CoroutineScope(bukkitDispatcher).launch {
                        OriginSelectorUI.open(
                            player = player,
                            layer = layer,
                            reason = OriginSelectorUI.OpenReason.INITIAL
                        )
                    }
                    break
                }
            }
        }
    }
}
