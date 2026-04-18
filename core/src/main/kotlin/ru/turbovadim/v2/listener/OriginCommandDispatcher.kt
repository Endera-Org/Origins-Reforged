package ru.turbovadim.v2.listener

import org.bukkit.Bukkit
import ru.turbovadim.OriginsReforged.Companion.mainConfig
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.event.OriginChangeReason
import ru.turbovadim.v2.event.OriginChangedEvent
import ru.turbovadim.v2.event.OriginChangedListener

/**
 * Runs the commands declared in `commandsOnOrigin` whenever a player's
 * origin changes (excluding DB restores).
 *
 * - Commands listed under the origin's name (matched case-insensitively) run.
 * - Commands listed under `"default"` run on every origin change.
 * - `%player%` is replaced with the player's username and `%uuid%` with their UUID.
 * - Commands are dispatched from the console on the main thread.
 */
class OriginCommandDispatcher(
    private val container: OriginsContainer
) : OriginChangedListener {

    override fun onChanged(event: OriginChangedEvent) {
        // Don't re-run commands when the player's origins are being re-applied from the DB.
        if (event.reason == OriginChangeReason.DATABASE_LOAD) return

        val newOrigin = event.newOrigin ?: return
        val player = event.player

        val map = mainConfig.commandsOnOrigin
        if (map.isEmpty()) return

        // Case-insensitive lookup by origin name.
        val matchingKey = map.keys.firstOrNull { it.equals(newOrigin.name, ignoreCase = true) }
        val defaultKey = map.keys.firstOrNull { it.equals("default", ignoreCase = true) }

        val commands = buildList {
            matchingKey?.let { map[it]?.let(::addAll) }
            defaultKey?.let { map[it]?.let(::addAll) }
        }
        if (commands.isEmpty()) return

        val console = Bukkit.getConsoleSender()
        val scheduler = Bukkit.getScheduler()
        val plugin = container.plugin

        // Dispatch on the main thread (scheduler will coalesce if we're already there).
        scheduler.runTask(plugin, Runnable {
            for (cmd in commands) {
                val resolved = cmd
                    .replace("%player%", player.name)
                    .replace("%uuid%", player.uniqueId.toString())
                try {
                    Bukkit.dispatchCommand(console, resolved)
                } catch (t: Throwable) {
                    plugin.logger.warning(
                        "commandsOnOrigin failed to run '$resolved': ${t.message}"
                    )
                }
            }
        })
    }
}
