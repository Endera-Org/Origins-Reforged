package ru.turbovadim.commands

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import ru.turbovadim.OrbOfOrigin
import ru.turbovadim.OriginsReforged.Companion.bukkitDispatcher
import ru.turbovadim.OriginsReforged.Companion.mainConfig
import ru.turbovadim.OriginsReforged.Companion.v2Container
import ru.turbovadim.ShortcutUtils
import ru.turbovadim.v2.event.OriginChangeReason
import ru.turbovadim.v2.ui.OriginSelectorUI

/**
 * Main command handler for /origin.
 *
 * Subcommands:
 * - /origin help - Show help
 * - /origin orb [player] [amount] - Give Orb(s) of Origin
 * - /origin set <player> <origin> [layer] - Set a player's origin
 * - /origin get [player] - Get a player's origin(s)
 * - /origin list [layer] - List available origins
 * - /origin swap [layer] - Open the origin selector to swap your origin
 * - /origin reload - Reload configuration
 */
class OriginCommand : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            return showHelp(sender)
        }

        return when (args[0].lowercase()) {
            "help" -> showHelp(sender)
            "orb" -> giveOrb(sender, args)
            "set" -> setOrigin(sender, args)
            "get" -> getOrigin(sender, args)
            "list" -> listOrigins(sender, args)
            "swap" -> swapOrigin(sender, args)
            "reload" -> reload(sender)
            else -> {
                sender.sendMessage(Component.text("Unknown subcommand. Use /origin help for a list of commands.", NamedTextColor.RED))
                true
            }
        }
    }

    private fun showHelp(sender: CommandSender): Boolean {
        sender.sendMessage(Component.text("=== Origins Commands ===", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("/origin help", NamedTextColor.YELLOW).append(Component.text(" - Show this help", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("/origin orb [player] [amount]", NamedTextColor.YELLOW).append(Component.text(" - Give Orb(s) of Origin", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("/origin set <player> <origin> [layer]", NamedTextColor.YELLOW).append(Component.text(" - Set a player's origin", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("/origin get [player]", NamedTextColor.YELLOW).append(Component.text(" - Get a player's origins", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("/origin list [layer]", NamedTextColor.YELLOW).append(Component.text(" - List available origins", NamedTextColor.GRAY)))
        if (mainConfig.swapCommand.enabled) {
            sender.sendMessage(Component.text("/origin swap [layer]", NamedTextColor.YELLOW).append(Component.text(" - Swap your current origin", NamedTextColor.GRAY)))
        }
        if (sender.hasPermission("originsreforged.admin")) {
            sender.sendMessage(Component.text("/origin reload", NamedTextColor.YELLOW).append(Component.text(" - Reload configuration", NamedTextColor.GRAY)))
        }
        return true
    }

    private fun swapOrigin(sender: CommandSender, args: Array<out String>): Boolean {
        val config = mainConfig

        if (!config.swapCommand.enabled) {
            sender.sendMessage(Component.text("The /origin swap command is disabled on this server.", NamedTextColor.RED))
            return true
        }

        if (sender !is Player) {
            sender.sendMessage(Component.text("Only players can use /origin swap.", NamedTextColor.RED))
            return true
        }

        if (!sender.hasPermission(config.swapCommand.permission)) {
            sender.sendMessage(ShortcutUtils.getColored(config.messages.noSwapCommandPermissions))
            return true
        }

        val container = v2Container ?: run {
            sender.sendMessage(Component.text("Origins system not initialized.", NamedTextColor.RED))
            return true
        }

        val requestedLayer = args.getOrNull(1)
        val layer = when {
            requestedLayer != null && requestedLayer in container.originLoader.layers -> requestedLayer
            requestedLayer != null -> {
                sender.sendMessage(Component.text("Unknown layer: $requestedLayer", NamedTextColor.RED))
                return true
            }
            else -> container.originLoader.layers.firstOrNull() ?: "origin"
        }

        CoroutineScope(bukkitDispatcher).launch {
            OriginSelectorUI.open(
                player = sender,
                layer = layer,
                reason = OriginSelectorUI.OpenReason.SWAP
            )
        }
        return true
    }

    private fun giveOrb(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("originsreforged.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED))
            return true
        }

        val target: Player = if (args.size >= 2) {
            Bukkit.getPlayer(args[1]) ?: run {
                sender.sendMessage(Component.text("Player not found: ${args[1]}", NamedTextColor.RED))
                return true
            }
        } else if (sender is Player) {
            sender
        } else {
            sender.sendMessage(Component.text("Please specify a player.", NamedTextColor.RED))
            return true
        }

        val amount = if (args.size >= 3) {
            args[2].toIntOrNull()?.coerceIn(1, 64) ?: run {
                sender.sendMessage(Component.text("Invalid amount: ${args[2]}", NamedTextColor.RED))
                return true
            }
        } else {
            1
        }

        val orbStack = OrbOfOrigin.orb.clone().apply { this.amount = amount }
        target.inventory.addItem(orbStack)

        val orbText = if (amount == 1) "an Orb of Origin" else "$amount Orbs of Origin"
        sender.sendMessage(Component.text("Gave $orbText to ${target.name}.", NamedTextColor.GREEN))
        return true
    }

    private fun setOrigin(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("originsreforged.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED))
            return true
        }

        if (args.size < 3) {
            sender.sendMessage(Component.text("Usage: /origin set <player> <origin> [layer]", NamedTextColor.RED))
            return true
        }

        val container = v2Container ?: run {
            sender.sendMessage(Component.text("Origins system not initialized.", NamedTextColor.RED))
            return true
        }

        val target = Bukkit.getPlayer(args[1]) ?: run {
            sender.sendMessage(Component.text("Player not found: ${args[1]}", NamedTextColor.RED))
            return true
        }

        val originName = args[2]
        val origin = container.originRegistry.getByName(originName) ?: run {
            sender.sendMessage(Component.text("Origin not found: $originName", NamedTextColor.RED))
            return true
        }

        val layer = if (args.size >= 4) args[3] else origin.layer

        container.playerStateManager.setOrigin(target, layer, origin, OriginChangeReason.COMMAND)
        sender.sendMessage(Component.text("Set ${target.name}'s origin to ${origin.getNameForDisplay()} in layer '$layer'.", NamedTextColor.GREEN))
        return true
    }

    private fun getOrigin(sender: CommandSender, args: Array<out String>): Boolean {
        val container = v2Container ?: run {
            sender.sendMessage(Component.text("Origins system not initialized.", NamedTextColor.RED))
            return true
        }

        val target: Player = if (args.size >= 2) {
            Bukkit.getPlayer(args[1]) ?: run {
                sender.sendMessage(Component.text("Player not found: ${args[1]}", NamedTextColor.RED))
                return true
            }
        } else if (sender is Player) {
            sender
        } else {
            sender.sendMessage(Component.text("Please specify a player.", NamedTextColor.RED))
            return true
        }

        val state = container.playerStateManager.getStateOrNull(target)
        if (state == null || state.origins.isEmpty()) {
            sender.sendMessage(Component.text("${target.name} has no origins.", NamedTextColor.YELLOW))
            return true
        }

        sender.sendMessage(Component.text("=== ${target.name}'s Origins ===", NamedTextColor.GOLD))
        for ((layer, origin) in state.origins) {
            sender.sendMessage(Component.text("$layer: ", NamedTextColor.YELLOW).append(Component.text(origin.getNameForDisplay(), NamedTextColor.WHITE)))
        }
        return true
    }

    private fun listOrigins(sender: CommandSender, args: Array<out String>): Boolean {
        val container = v2Container ?: run {
            sender.sendMessage(Component.text("Origins system not initialized.", NamedTextColor.RED))
            return true
        }

        val layer = if (args.size >= 2) args[1] else null

        if (layer != null) {
            val origins = container.originRegistry.getByLayer(layer)
            if (origins.isEmpty()) {
                sender.sendMessage(Component.text("No origins found in layer '$layer'.", NamedTextColor.YELLOW))
                return true
            }

            sender.sendMessage(Component.text("=== Origins in '$layer' ===", NamedTextColor.GOLD))
            for (origin in origins) {
                val choosable = if (origin.choosable) "" else " (not choosable)"
                sender.sendMessage(Component.text("- ${origin.getNameForDisplay()}$choosable", NamedTextColor.WHITE))
            }
        } else {
            val allOrigins = container.originRegistry.getAll()
            if (allOrigins.isEmpty()) {
                sender.sendMessage(Component.text("No origins registered.", NamedTextColor.YELLOW))
                return true
            }

            sender.sendMessage(Component.text("=== All Origins (${allOrigins.size}) ===", NamedTextColor.GOLD))
            val byLayer = allOrigins.groupBy { it.layer }
            for ((layerName, origins) in byLayer) {
                sender.sendMessage(Component.text("Layer '$layerName':", NamedTextColor.YELLOW))
                for (origin in origins) {
                    sender.sendMessage(Component.text("  - ${origin.getNameForDisplay()}", NamedTextColor.WHITE))
                }
            }
        }
        return true
    }

    private fun reload(sender: CommandSender): Boolean {
        if (!sender.hasPermission("originsreforged.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED))
            return true
        }

        val container = v2Container ?: run {
            sender.sendMessage(Component.text("Origins system not initialized.", NamedTextColor.RED))
            return true
        }

        // Reload origin files
        container.originLoader.reloadAll()
        sender.sendMessage(Component.text("Origins configuration reloaded.", NamedTextColor.GREEN))
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String>? {
        val container = v2Container ?: return emptyList()

        return when (args.size) {
            1 -> {
                val subcommands = mutableListOf("help", "orb", "get", "list")
                if (mainConfig.swapCommand.enabled && sender.hasPermission(mainConfig.swapCommand.permission)) {
                    subcommands.add("swap")
                }
                if (sender.hasPermission("originsreforged.admin")) {
                    subcommands.addAll(listOf("set", "reload"))
                }
                subcommands.filter { it.startsWith(args[0].lowercase()) }
            }
            2 -> {
                when (args[0].lowercase()) {
                    "orb", "set", "get" -> Bukkit.getOnlinePlayers().map { it.name }.filter { it.lowercase().startsWith(args[1].lowercase()) }
                    "list", "swap" -> container.originRegistry.layers.filter { it.lowercase().startsWith(args[1].lowercase()) }
                    else -> emptyList()
                }
            }
            3 -> {
                when (args[0].lowercase()) {
                    "set" -> container.originRegistry.getAll().map { it.name }.filter { it.lowercase().startsWith(args[2].lowercase()) }
                    "orb" -> listOf("1", "16", "32", "64").filter { it.startsWith(args[2]) }
                    else -> emptyList()
                }
            }
            4 -> {
                when (args[0].lowercase()) {
                    "set" -> container.originRegistry.layers.filter { it.lowercase().startsWith(args[3].lowercase()) }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }
}
