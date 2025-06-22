package ru.turbovadim.commands

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.AddonLoader
import ru.turbovadim.AddonLoader.allowOriginSwapCommand
import ru.turbovadim.AddonLoader.getOrigin
import ru.turbovadim.AddonLoader.getOrigins
import ru.turbovadim.AddonLoader.reloadAddons
import ru.turbovadim.OrbOfOrigin
import ru.turbovadim.OriginSwapper.Companion.getOrigin
import ru.turbovadim.OriginSwapper.Companion.openOriginSwapper
import ru.turbovadim.OriginSwapper.Companion.setOrigin
import ru.turbovadim.OriginsRebornEnhanced
import ru.turbovadim.OriginsRebornEnhanced.Companion.getCooldowns
import ru.turbovadim.PackApplier.Companion.sendPacks
import ru.turbovadim.config.ConfigRegistry
import ru.turbovadim.cooldowns.Cooldowns.CooldownInfo
import ru.turbovadim.events.PlayerSwapOriginEvent
import ru.turbovadim.util.CompressionUtils
import ru.turbovadim.util.CompressionUtils.decompressFiles
import ru.turbovadim.util.testBenchmarks
import java.io.File
import java.io.IOException

class OriginCommand : CommandExecutor, TabCompleter {

    companion object {
        @JvmField
        var key: NamespacedKey = getCooldowns().registerCooldown(
            OriginsRebornEnhanced.instance,
            NamespacedKey(OriginsRebornEnhanced.instance, "swap-command-cooldown"),
            CooldownInfo(0)
        )

        // Command names
        private const val CMD_BENCH = "bench"
        private const val CMD_SWAP = "swap"
        private const val CMD_RELOAD = "reload"
        private const val CMD_EXCHANGE = "exchange"
        private const val CMD_SET = "set"
        private const val CMD_ORB = "orb"
        private const val CMD_CHECK = "check"
        private const val CMD_PACK = "pack"
        private const val CMD_EXPORT = "export"
        private const val CMD_IMPORT = "import"

        // Messages
        private const val MSG_INVALID_COMMAND = "Invalid command. Usage: /origin <command>"
        private const val MSG_NO_PERMISSION = "You don't have permission to do this!"
        private const val MSG_PLAYER_ONLY = "This command can only be run by a player"
        private const val MSG_ON_COOLDOWN = "You are on cooldown."
        private const val MSG_COMMAND_DISABLED = "This command has been disabled in the configuration"
    }

    private val exchangeRequests: MutableMap<Player, MutableList<ExchangeRequest>> = HashMap()

    data class ExchangeRequest(val p1: Player?, val p2: Player?, val expireTime: Int, val layer: String)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(Component.text(MSG_INVALID_COMMAND).color(NamedTextColor.RED))
            return true
        }

        return when (args[0].lowercase()) {
            CMD_BENCH -> handleBenchCommand(sender)
            CMD_SWAP -> handleSwapCommand(sender, args)
            CMD_RELOAD -> handleReloadCommand(sender)
            CMD_EXCHANGE -> handleExchangeCommand(sender, args)
            CMD_SET -> handleSetCommand(sender, args)
            CMD_ORB -> handleOrbCommand(sender, args)
            CMD_CHECK -> handleCheckCommand(sender, args)
            CMD_PACK -> handlePackCommand(sender)
            CMD_EXPORT -> handleExportCommand(sender, args)
            CMD_IMPORT -> handleImportCommand(sender, args)
            else -> {
                sender.sendMessage(Component.text(MSG_INVALID_COMMAND).color(NamedTextColor.RED))
                true
            }
        }
    }

    private fun handleBenchCommand(sender: CommandSender): Boolean {
        val player = sender as? Player ?: return true
        testBenchmarks(player, player.location, 48.0)
        sender.sendMessage(Component.text("finished"))
        return true
    }

    private fun handleSwapCommand(sender: CommandSender, args: Array<String>): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage(Component.text(MSG_PLAYER_ONLY).color(NamedTextColor.RED))
            return true
        }

        if (getCooldowns().hasCooldown(player, key)) {
            player.sendMessage(Component.text(MSG_ON_COOLDOWN).color(NamedTextColor.RED))
            return true
        }

        if (!OriginsRebornEnhanced.mainConfig.swapCommand.enabled) {
            player.sendMessage(Component.text(MSG_COMMAND_DISABLED).color(NamedTextColor.RED))
            return true
        }

        if (!allowOriginSwapCommand(player)) {
            player.sendMessage(Component.text(OriginsRebornEnhanced.mainConfig.messages.noSwapCommandPermissions))
            return true
        }

        val layer = if (args.size == 2) args[1] else "origin"
        openOriginSwapper(
            player,
            PlayerSwapOriginEvent.SwapReason.COMMAND,
            0,
            0,
            OriginsRebornEnhanced.instance.isVaultEnabled,
            layer
        )
        return true
    }

    private fun handleReloadCommand(sender: CommandSender): Boolean {
        if (!hasAdminPermission(sender)) return true

        reloadAddons()
        OriginsRebornEnhanced.multiConfigurationManager.loadAllConfigs().forEach { (clazz, config) ->
            ConfigRegistry.register(clazz, config)
        }
        return true
    }

    private fun handleExchangeCommand(sender: CommandSender, args: Array<String>): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage(Component.text(MSG_PLAYER_ONLY).color(NamedTextColor.RED))
            return true
        }

        if (!player.hasPermission("originsreborn.exchange")) {
            player.sendMessage(Component.text(MSG_NO_PERMISSION).color(NamedTextColor.RED))
            return true
        }

        if (args.size < 2) {
            player.sendMessage(Component.text("Usage: /origin exchange <player> [<layer>]").color(NamedTextColor.RED))
            return true
        }

        val target = Bukkit.getPlayer(args[1]) ?: run {
            player.sendMessage(Component.text("Usage: /origin exchange <player> [<layer>]").color(NamedTextColor.RED))
            return true
        }

        if (target == player) {
            player.sendMessage(Component.text("You must specify another player.").color(NamedTextColor.RED))
            return true
        }

        return processExchangeRequest(player, target, args)
    }

    private fun processExchangeRequest(player: Player, target: Player, args: Array<String>): Boolean {
        val layer = if (args.size != 3) "origin" else args[2]

        // Check for existing requests
        for (request in exchangeRequests.getOrDefault(player, mutableListOf())!!) {
            if (request.expireTime <= Bukkit.getCurrentTick()) continue

            if (request.p2 == player && request.p1 == target) {
                executeExchange(player, target, request.layer)
                return true
            }
        }

        // Create new request
        if (target !in exchangeRequests) {
            exchangeRequests[target] = mutableListOf()
        }

        exchangeRequests[target]!!.removeIf { request ->
            request.p1 == player && request.p2 == player
        }

        exchangeRequests[target]!!.add(
            ExchangeRequest(player, target, Bukkit.getCurrentTick() + 6000, layer)
        )

        sendExchangeMessages(player, target, layer)
        return true
    }

    private fun executeExchange(player: Player, target: Player, layer: String) {
        val layerCapitalized = layer.replaceFirstChar { it.uppercase() }

        target.sendMessage(Component.text("$layerCapitalized swapped with ${player.name}.").color(NamedTextColor.AQUA))
        player.sendMessage(Component.text("$layerCapitalized swapped with ${target.name}.").color(NamedTextColor.AQUA))

        CoroutineScope(ioDispatcher).launch {
            val pOrigin = getOrigin(player, layer)
            val tOrigin = getOrigin(target, layer)

            setOrigin(player, tOrigin, PlayerSwapOriginEvent.SwapReason.COMMAND, false, layer)
            setOrigin(target, pOrigin, PlayerSwapOriginEvent.SwapReason.COMMAND, false, layer)
        }
    }

    private fun sendExchangeMessages(player: Player, target: Player, layer: String) {
        target.sendMessage(
            Component.text(
                "$layer is requesting to swap ${player.name} with you, type /origin exchange ${player.name} to accept. The request will expire in 5 minutes."
            ).color(NamedTextColor.AQUA)
        )
        player.sendMessage(
            Component.text(
                "Requesting to swap $layer with ${target.name}. The request will expire in 5 minutes."
            ).color(NamedTextColor.AQUA)
        )
    }

    private fun handleSetCommand(sender: CommandSender, args: Array<String>): Boolean {
        if (!hasAdminPermission(sender)) return true

        if (args.size < 4) {
            sender.sendMessage(
                Component.text("Invalid command. Usage: /origin set <player> <layer> <origin>")
                    .color(NamedTextColor.RED)
            )
            return true
        }

        val player = Bukkit.getPlayer(args[1]) ?: run {
            sender.sendMessage(
                Component.text("Invalid command. Usage: /origin set <player> <layer> <origin>")
                    .color(NamedTextColor.RED)
            )
            return true
        }

        val layer = args[2]
        val origin = getOrigin(args[3].replace("_", " ")) ?: run {
            sender.sendMessage(
                Component.text("Invalid command. Usage: /origin set <player> <layer> <origin>")
                    .color(NamedTextColor.RED)
            )
            return true
        }

        if (origin.layer != layer) {
            sender.sendMessage(
                Component.text("Invalid command. Usage: /origin set <player> <layer> <origin>")
                    .color(NamedTextColor.RED)
            )
            return true
        }

        CoroutineScope(ioDispatcher).launch {
            setOrigin(player, origin, PlayerSwapOriginEvent.SwapReason.COMMAND, false, layer)
        }
        return true
    }

    private fun handleOrbCommand(sender: CommandSender, args: Array<String>): Boolean {
        val player: Player = when {
            sender is Player -> {
                if (!sender.hasPermission("originsreborn.admin")) {
                    sender.sendMessage(Component.text(MSG_NO_PERMISSION).color(NamedTextColor.RED))
                    return true
                }
                sender
            }
            args.size == 2 -> {
                Bukkit.getPlayer(args[1]) ?: run {
                    sender.sendMessage(Component.text(MSG_PLAYER_ONLY).color(NamedTextColor.RED))
                    return true
                }
            }
            else -> {
                sender.sendMessage(Component.text(MSG_PLAYER_ONLY).color(NamedTextColor.RED))
                return true
            }
        }

        player.inventory.addItem(OrbOfOrigin.orb)
        return true
    }

    private fun handleCheckCommand(sender: CommandSender, args: Array<String>): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage(Component.text(MSG_PLAYER_ONLY).color(NamedTextColor.RED))
            return true
        }

        val layer = if (args.size == 2) args[1] else "origin"
        openOriginSwapper(
            player = player,
            reason = PlayerSwapOriginEvent.SwapReason.COMMAND,
            getOrigins(layer).indexOf(runBlocking { getOrigin(player, layer) }),
            scrollAmount = 0,
            cost = false,
            displayOnly = true,
            layer = layer
        )
        return true
    }

    private fun handlePackCommand(sender: CommandSender): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage(Component.text(MSG_PLAYER_ONLY).color(NamedTextColor.RED))
            return true
        }

        sendPacks(player)
        return true
    }

    private fun handleExportCommand(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size != 3) {
            sender.sendMessage(
                Component.text("Invalid command. Usage: /origin export <addon id> <path>")
                    .color(NamedTextColor.RED)
            )
            return true
        }
        val output = File(OriginsRebornEnhanced.instance.dataFolder, "export/${args[2]}.orbarch")
        val files = AddonLoader.originFiles[args[1]]
        if (files == null) {
            sender.sendMessage(
                Component.text("Invalid command. Usage: /origin export <addon id> <path>")
                    .color(NamedTextColor.RED)
            )
            return true
        }
        // Фильтруем null-элементы
        val nonNullFiles = files.toMutableList()
        try {
            CompressionUtils.compressFiles(nonNullFiles, output)
            sender.sendMessage(
                Component.text("Exported origins to '~/plugins/Origins-Reborn/export/${args[2]}.orbarch'")
                    .color(NamedTextColor.AQUA)
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return true
    }

    private fun handleImportCommand(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size != 2) {
            sender.sendMessage(
                Component.text("Invalid command. Usage: /origin import <path>").color(NamedTextColor.RED)
            )
            return true
        }
        val input = File(OriginsRebornEnhanced.instance.dataFolder, "import/" + args[1])
        val output = File(OriginsRebornEnhanced.instance.dataFolder, "origins")
        if (!input.exists() || !output.exists()) {
            sender.sendMessage(
                Component.text("Invalid command. Usage: /origin import <path>").color(NamedTextColor.RED)
            )
            return true
        }
        try {
            decompressFiles(input, output)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): MutableList<String?> {
        val result = ArrayList<String?>()
        val data = getTabCompletionData(sender, args)
        StringUtil.copyPartialMatches(args[args.size - 1], data, result)
        return result
    }

    private fun getTabCompletionData(sender: CommandSender, args: Array<String>): List<String?> {
        return when (args.size) {
            1 -> getFirstArgumentCompletions(sender)
            2 -> getSecondArgumentCompletions(args[0])
            3 -> getThirdArgumentCompletions(args[0])
            4 -> getFourthArgumentCompletions(args[0], args[2])
            else -> emptyList()
        }
    }

    private fun getFirstArgumentCompletions(sender: CommandSender): List<String?> {
        val completions = mutableListOf<String?>()
        completions.add(CMD_CHECK)

        if (sender is Player && allowOriginSwapCommand(sender)) {
            completions.add(CMD_SWAP)
        }

        if (sender.hasPermission("originsreborn.exchange")) {
            completions.add(CMD_EXCHANGE)
        }

        if (sender.hasPermission("originsreborn.admin")) {
            completions.addAll(listOf(CMD_RELOAD, CMD_SET, CMD_ORB, CMD_EXPORT, CMD_IMPORT, CMD_PACK))
        }

        return completions
    }

    private fun getSecondArgumentCompletions(firstArg: String): List<String?> {
        return when (firstArg) {
            CMD_SET, CMD_ORB, CMD_EXCHANGE -> Bukkit.getOnlinePlayers().map { it.name }
            CMD_EXPORT -> ArrayList(AddonLoader.originFiles.keys)
            CMD_CHECK, CMD_SWAP -> ArrayList(AddonLoader.layers)
            CMD_IMPORT -> getImportFileNames()
            else -> emptyList()
        }
    }

    private fun getThirdArgumentCompletions(firstArg: String): List<String?> {
        return if (firstArg == CMD_SET) ArrayList(AddonLoader.layers) else emptyList()
    }

    private fun getFourthArgumentCompletions(firstArg: String, layer: String): List<String?> {
        return if (firstArg == CMD_SET) {
            getOrigins(layer).map { origin ->
                origin.getName().lowercase().replace(" ", "_")
            }
        } else emptyList()
    }

    private fun getImportFileNames(): List<String?> {
        val input = File(OriginsRebornEnhanced.instance.dataFolder, "import")
        val files = input.listFiles() ?: return emptyList()
        return files.map { it.name }
    }

    private fun hasAdminPermission(sender: CommandSender): Boolean {
        if (sender is Player && !sender.hasPermission("originsreborn.admin")) {
            sender.sendMessage(Component.text(MSG_NO_PERMISSION).color(NamedTextColor.RED))
            return false
        }
        return true
    }
}
