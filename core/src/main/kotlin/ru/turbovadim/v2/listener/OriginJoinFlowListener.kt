package ru.turbovadim.v2.listener

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.endera.enderalib.utils.async.runTask
import org.endera.enderalib.utils.async.runTaskLater
import ru.turbovadim.OriginsReforged
import ru.turbovadim.OriginsReforged.Companion.bukkitDispatcher
import ru.turbovadim.OriginsReforged.Companion.mainConfig
import ru.turbovadim.ShortcutUtils
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.event.OriginChangeReason
import ru.turbovadim.v2.ui.OriginSelectorUI

/**
 * Drives the initial / on-join origin selection flow.
 *
 * On join (delayed by `originSelection.delayBeforeRequired` ticks — or
 * `geyser.joinFormDelay` for Bedrock players), for each missing layer in
 * priority order:
 *
 * 1. If `originSelection.randomize[layer] == true`, assign a random origin.
 * 2. Else if `originSelection.defaultOrigin[layer]` resolves to a registered
 *    origin, assign that.
 * 3. Else open the selection GUI for that layer (one GUI at a time).
 *
 * After the first origin is applied to a player who had none, teleports
 * them to the world spawn if `originSelection.autoSpawnTeleport == true`.
 */
class OriginJoinFlowListener(
    private val container: OriginsContainer
) : Listener {

    private val plugin get() = container.plugin

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Choose the appropriate delay (Bedrock players may need longer).
        val delay = if (ShortcutUtils.isBedrockPlayer(player.uniqueId)) {
            mainConfig.geyser.joinFormDelay.coerceAtLeast(1)
        } else {
            mainConfig.originSelection.delayBeforeRequired.coerceAtLeast(1)
        }

        scheduleFlow(player, delay.toLong(), remainingPolls = 100)
    }

    /**
     * Schedule the flow. If the player's DB load hasn't completed yet, poll
     * once per tick until it does (up to ~5 seconds).
     */
    private fun scheduleFlow(player: Player, initialDelay: Long, remainingPolls: Int) {
        // Entity-tied: polls follow the player across regions on Folia.
        player.runTaskLater(plugin, initialDelay) {
            if (!player.isOnline) return@runTaskLater

            val state = container.playerStateManager.getStateOrNull(player)
            if (state == null || !state.dbLoadComplete) {
                if (remainingPolls > 0) {
                    scheduleFlow(player, 1L, remainingPolls - 1)
                } else {
                    plugin.logger.warning(
                        "DB origin load for ${player.name} didn't complete in time; running join flow anyway."
                    )
                    runJoinFlow(player)
                }
                return@runTaskLater
            }
            runJoinFlow(player)
        }
    }

    private fun runJoinFlow(player: Player) {
        if (!player.isOnline) return

        val hadAnyOriginBefore = container.playerStateManager
            .getStateOrNull(player)?.origins?.isNotEmpty() == true

        val layers = container.originLoader.layers
        if (layers.isEmpty()) return

        var openedGui = false
        var assignedFreshly = false

        for (layer in layers) {
            val state = container.playerStateManager.getState(player)
            if (state.hasOriginForLayer(layer)) continue

            // 1. randomize?
            if (container.originLoader.isRandomOnSelection(layer)) {
                val origin = container.originRegistry.getRandomOrigin(layer)
                if (origin != null) {
                    container.playerStateManager.setOrigin(player, layer, origin, OriginChangeReason.PLUGIN)
                    assignedFreshly = true
                    continue
                }
            }

            // 2. defaultOrigin?
            val defaultName = container.originLoader.getDefaultOriginName(layer)
            if (defaultName != null) {
                val origin = container.originRegistry.getByName(defaultName)
                if (origin != null) {
                    container.playerStateManager.setOrigin(player, layer, origin, OriginChangeReason.PLUGIN)
                    assignedFreshly = true
                    continue
                } else {
                    plugin.logger.warning(
                        "Default origin '$defaultName' for layer '$layer' is not registered."
                    )
                }
            }

            // 3. open GUI (only the first missing layer gets one; stop after that)
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

        // autoSpawnTeleport: only on the player's first-ever origin assignment.
        // Folia-safe: teleportAsync works on both Paper and Folia; the confirmation message
        // is hopped back onto the player's region thread once the teleport completes.
        if (!hadAnyOriginBefore && assignedFreshly && mainConfig.originSelection.autoSpawnTeleport) {
            val spawn = player.world.spawnLocation
            player.teleportAsync(spawn).thenAccept { success ->
                if (success == true) {
                    player.runTask(plugin) {
                        player.sendMessage(Component.text("Teleported to spawn.", NamedTextColor.GREEN))
                    }
                }
            }
        }
    }
}
