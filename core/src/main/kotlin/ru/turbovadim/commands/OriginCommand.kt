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
import java.util.*

class OriginCommand : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Invalid command. Usage: /origin <command>").color(NamedTextColor.RED))
            return true
        }
        when (args[0].lowercase(Locale.getDefault())) {
            "bench" -> {
                if (sender !is Player) return true
                testBenchmarks(sender, sender.location, 48.0)
                sender.sendMessage(Component.text("finished"))
                return true
            }
            "swap" -> {
                if (sender is Player) {
                    if (getCooldowns().hasCooldown(sender, key)) {
                        sender.sendMessage(Component.text("You are on cooldown.").color(NamedTextColor.RED))
                        return true
                    }
                    if (OriginsRebornEnhanced.mainConfig.swapCommand.enabled) {
                        if (allowOriginSwapCommand(sender)) {
                            val layer = if (args.size == 2) args[1]
                            else "origin"
                            openOriginSwapper(
                                sender,
                                PlayerSwapOriginEvent.SwapReason.COMMAND,
                                0,
                                0,
                                OriginsRebornEnhanced.instance.isVaultEnabled,
                                layer
                            )
                        } else {
                            sender.sendMessage(Component.text(OriginsRebornEnhanced.mainConfig.messages.noSwapCommandPermissions))
                        }
                    } else {
                        sender.sendMessage(
                            Component.text("This command has been disabled in the configuration").color(
                                NamedTextColor.RED
                            )
                        )
                    }
                } else {
                    sender.sendMessage(
                        Component.text("This command can only be run by a player").color(NamedTextColor.RED)
                    )
                }
                return true
            }

            "reload" -> {
                if (sender is Player) {
                    if (!sender.hasPermission("originsreborn.admin")) {
                        sender.sendMessage(
                            Component.text("You don't have permission to do this!").color(NamedTextColor.RED)
                        )
                        return true
                    }
                }
                reloadAddons()
                OriginsRebornEnhanced.multiConfigurationManager.loadAllConfigs().forEach { (clazz, config) ->
                    ConfigRegistry.register(clazz, config)
                }
                return true
            }

            "exchange" -> {
                if (sender is Player) {
                    if (sender.hasPermission("originsreborn.exchange")) {
                        if (args.size < 2) {
                            sender.sendMessage(
                                Component.text("Usage: /origin exchange <player> [<layer>]").color(
                                    NamedTextColor.RED
                                )
                            )
                            return true
                        }
                        val target = Bukkit.getPlayer(args[1])
                        if (target == null) {
                            sender.sendMessage(
                                Component.text("Usage: /origin exchange <player> [<layer>]").color(
                                    NamedTextColor.RED
                                )
                            )
                            return true
                        }
                        if (target == sender) {
                            sender.sendMessage(
                                Component.text("You must specify another player.").color(NamedTextColor.RED)
                            )
                            return true
                        }
                        for (request in exchangeRequests.getOrDefault(sender, mutableListOf<ExchangeRequest?>())!!) {
                            if (request!!.expireTime > Bukkit.getCurrentTick()) continue
                            val l = request.layer.substring(0, 0)
                                .uppercase(Locale.getDefault()) + request.layer.substring(1)
                            val layer = request.layer
                            if (request.p2 == sender && request.p1 == target) {
                                target.sendMessage(
                                    Component.text("$l swapped with ${sender.name}.").color(
                                        NamedTextColor.AQUA
                                    )
                                )
                                sender.sendMessage(
                                    Component.text("$l swapped with ${target.name}.").color(
                                        NamedTextColor.AQUA
                                    )
                                )

                                CoroutineScope(ioDispatcher).launch {
                                    val pOrigin = getOrigin(sender, layer)
                                    val tOrigin = getOrigin(target, layer)

                                    setOrigin(sender, tOrigin, PlayerSwapOriginEvent.SwapReason.COMMAND, false, layer)
                                    setOrigin(target, pOrigin, PlayerSwapOriginEvent.SwapReason.COMMAND, false, layer)
                                }
                                return true
                            }
                        }
                        if (target !in exchangeRequests) {
                            exchangeRequests[target] = mutableListOf()
                        }
                        exchangeRequests[target]!!
                            .removeIf { request: ExchangeRequest? -> request!!.p1 == sender && request.p2 == sender }
                        val layer = if (args.size != 3) "origin"
                        else args[2]

                        exchangeRequests[target]!!
                            .add(ExchangeRequest(sender, target, Bukkit.getCurrentTick() + 6000, layer))
                        target.sendMessage(
                            Component.text(
                                "$layer is requesting to swap ${sender.name} with you, type /origin exchange ${sender.name} to accept. The request will expire in 5 minutes."
                            ).color(
                                NamedTextColor.AQUA
                            )
                        )
                        sender.sendMessage(
                            Component.text(
                                "Requesting to swap $layer with ${target.name}. The request will expire in 5 minutes."
                            ).color(
                                NamedTextColor.AQUA
                            )
                        )
                    } else {
                        sender.sendMessage(
                            Component.text("You don't have permission to do this!").color(NamedTextColor.RED)
                        )
                    }
                } else {
                    sender.sendMessage(
                        Component.text("Only players can switch origins with others!").color(
                            NamedTextColor.RED
                        )
                    )
                }
                return true
            }

            "set" -> {
                if (sender is Player) {
                    if (!sender.hasPermission("originsreborn.admin")) {
                        sender.sendMessage(
                            Component.text("You don't have permission to do this!").color(NamedTextColor.RED)
                        )
                        return true
                    }
                }
                if (args.size < 4) {
                    sender.sendMessage(
                        Component.text("Invalid command. Usage: /origin set <player> <layer> <origin>").color(
                            NamedTextColor.RED
                        )
                    )
                    return true
                }
                val layer = args[2]
                val player = Bukkit.getPlayer(args[1])
                if (player == null) {
                    sender.sendMessage(
                        Component.text("Invalid command. Usage: /origin set <player> <layer> <origin>").color(
                            NamedTextColor.RED
                        )
                    )
                    return true
                }
                val origin = getOrigin(args[3].replace("_", " "))
                if (origin == null || origin.layer != layer) {
                    sender.sendMessage(
                        Component.text("Invalid command. Usage: /origin set <player> <layer> <origin>").color(
                            NamedTextColor.RED
                        )
                    )
                    return true
                }
                CoroutineScope(ioDispatcher).launch {
                    setOrigin(player, origin, PlayerSwapOriginEvent.SwapReason.COMMAND, false, layer)
                }
                return true
            }

            "orb" -> {
                val player: Player = if (sender is Player) {
                    if (!sender.hasPermission("originsreborn.admin")) {
                        sender.sendMessage(
                            Component.text("You don't have permission to do this!")
                                .color(NamedTextColor.RED)
                        )
                        return true
                    }
                    sender
                } else {
                    if (args.size != 2) {
                        sender.sendMessage(
                            Component.text("This command can only be run by a player")
                                .color(NamedTextColor.RED)
                        )
                        return true
                    }
                    Bukkit.getPlayer(args[1]) ?: run {
                        sender.sendMessage(
                            Component.text("This command can only be run by a player")
                                .color(NamedTextColor.RED)
                        )
                        return true
                    }
                }
                player.inventory.addItem(OrbOfOrigin.orb)
                return true
            }


            "check" -> {
                if (sender is Player) {
                    val layer = if (args.size == 2) args[1]
                    else "origin"
                        openOriginSwapper(
                            player = sender,
                            reason = PlayerSwapOriginEvent.SwapReason.COMMAND, getOrigins(layer).indexOf(
                                runBlocking { getOrigin(sender, layer) }
                            ),
                            scrollAmount = 0,
                            cost = false,
                            displayOnly = true,
                            layer = layer
                        )
                } else {
                    sender.sendMessage(
                        Component.text("This command can only be run by a player").color(NamedTextColor.RED)
                    )
                }
                return true
            }

            "pack" -> {
                if (sender is Player) {
                    sendPacks(sender)
                } else {
                    sender.sendMessage(
                        Component.text("This command can only be run by a player").color(NamedTextColor.RED)
                    )
                }
                return true
            }

            "export" -> {
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
                val nonNullFiles = files.filterNotNull().toMutableList()
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

            "import" -> {
                if (args.size != 2) {
                    sender.sendMessage(
                        Component.text("Invalid command. Usage: /origin import <path>").color(
                            NamedTextColor.RED
                        )
                    )
                    return true
                }
                val input = File(OriginsRebornEnhanced.instance.dataFolder, "import/" + args[1])
                val output = File(OriginsRebornEnhanced.instance.dataFolder, "origins")
                if (!input.exists() || !output.exists()) {
                    sender.sendMessage(
                        Component.text("Invalid command. Usage: /origin import <path>").color(
                            NamedTextColor.RED
                        )
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

            else -> {
                sender.sendMessage(
                    Component.text("Invalid command. Usage: /origin <command>").color(NamedTextColor.RED)
                )
                return true
            }
        }
    }

    private val exchangeRequests: MutableMap<Player?, MutableList<ExchangeRequest>?> =
        HashMap<Player?, MutableList<ExchangeRequest>?>()

    @JvmRecord
    data class ExchangeRequest(val p1: Player?, val p2: Player?, val expireTime: Int, val layer: String)

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): MutableList<String?>? {
        val result: MutableList<String?> = ArrayList<String?>()
        val data = when (args.size) {
            1 -> {
                val r: MutableList<String?> = ArrayList<String?>()
                r.add("check")
                if (sender is Player && allowOriginSwapCommand(sender)) {
                    r.add("swap")
                }
                if (sender.hasPermission("originsreborn.exchange")) {
                    r.add("exchange")
                }
                if (!sender.hasPermission("originsreborn.admin")) r
                r.add("reload")
                r.add("set")
                r.add("orb")
                r.add("export")
                r.add("import")
                r.add("pack")
                r
            }

            2 -> {
                when (args[0]) {
                    "set", "orb", "exchange" -> {
                        object : ArrayList<String?>() {
                            init {
                                for (player in Bukkit.getOnlinePlayers()) {
                                    add(player.name)
                                }
                            }
                        }
                    }

                    "export" -> {
                        ArrayList<String?>(AddonLoader.originFiles.keys)
                    }

                    "check", "swap" -> {
                        ArrayList<String?>(AddonLoader.layers)
                    }

                    "import" -> {
                        val input = File(OriginsRebornEnhanced.instance.dataFolder, "import")
                        val files = input.listFiles()

                        if (files == null) mutableListOf<String?>()
                        val fileNames: MutableList<String?> = ArrayList<String?>()
                        for (file in files!!) {
                            fileNames.add(file.getName())
                        }
                        fileNames
                    }

                    else -> {
                        mutableListOf<String?>()
                    }
                }
            }

            3 -> {
                if (args[0] == "set") {
                    ArrayList<String?>(AddonLoader.layers)
                } else mutableListOf<String?>()
            }

            4 -> {
                if (args[0] == "set") {
                    val layer = args[2]
                    object : ArrayList<String?>() {
                        init {
                            for (origin in getOrigins(layer)) {
                                checkNotNull(origin)
                                add(origin.getName().lowercase(Locale.getDefault()).replace(" ", "_"))
                            }
                        }
                    }
                } else mutableListOf<String?>()
            }

            else -> mutableListOf<String?>()
        }
        StringUtil.copyPartialMatches<MutableList<String?>?>(args[args.size - 1], data, result)
        return result
    }

    companion object {
        @JvmField
        var key: NamespacedKey = getCooldowns().registerCooldown(
            OriginsRebornEnhanced.instance,
            NamespacedKey(OriginsRebornEnhanced.instance, "swap-command-cooldown"),
            CooldownInfo(0)
        )
    }
}