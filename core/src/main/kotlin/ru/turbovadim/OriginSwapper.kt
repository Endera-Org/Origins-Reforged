package ru.turbovadim

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import fr.xephi.authme.api.v3.AuthMeApi
import kotlinx.coroutines.*
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.AddonLoader.getDefaultOrigin
import ru.turbovadim.AddonLoader.getFirstOrigin
import ru.turbovadim.AddonLoader.getFirstUnselectedLayer
import ru.turbovadim.AddonLoader.getOriginByFilename
import ru.turbovadim.AddonLoader.getRandomOrigin
import ru.turbovadim.AddonLoader.shouldOpenSwapMenu
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.OriginsReforged.Companion.bukkitDispatcher
import ru.turbovadim.OriginsReforged.Companion.getCooldowns
import ru.turbovadim.OriginsReforged.Companion.instance
import ru.turbovadim.abilities.AbilityRegister
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.DefaultSpawnAbility
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.commands.OriginCommand
import ru.turbovadim.database.DatabaseManager
import ru.turbovadim.events.PlayerSwapOriginEvent
import ru.turbovadim.events.PlayerSwapOriginEvent.SwapReason
import ru.turbovadim.geysermc.GeyserSwapper
import ru.turbovadim.packetsenders.NMSInvoker
import ru.turbovadim.utils.CustomGuiFactory
import java.util.*
import kotlin.math.max
import kotlin.math.min

class OriginSwapper : Listener {

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.reason in listOf(InventoryCloseEvent.Reason.OPEN_NEW, InventoryCloseEvent.Reason.CANT_USE, InventoryCloseEvent.Reason.PLUGIN)) return
        if (event.inventory.holder !is CustomGuiFactory) return
        val player = event.player as Player

        runBlocking {
            val displayItem = event.inventory.getItem(1)
            if (displayItem != null) {
                val layer = displayItem.itemMeta.persistentDataContainer
                    .getOrDefault(layerKey, PersistentDataType.STRING, "origin")

                val reason = getReason(displayItem)

                val shouldPreventClose = reason == SwapReason.ORB_OF_ORIGIN || hasNotSelectedAllOrigins(player)

                if (shouldPreventClose) {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(instance, {
                        if (player.isOnline) {
                            openOriginSwapper(player, reason, 0, 0, layer)
                        }
                    }, 1L)
                }
            }
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.clickedInventory?.holder !is CustomGuiFactory?) return
        runBlocking {
            val config = OriginsReforged.mainConfig
            val item = event.whoClicked.openInventory.getItem(1)
            if (item != null) {
                val meta = item.itemMeta
                if (meta == null) return@runBlocking
                val itemContainer = meta.persistentDataContainer
                if (itemContainer.has(displayKey, BooleanPDT.Companion.BOOLEAN)) {
                    event.isCancelled = true
                }
                val layer = itemContainer.getOrDefault(layerKey, PersistentDataType.STRING, "origin")
                val player = event.whoClicked
                if (player is Player) {
                    val currentItem = event.currentItem
                    if (currentItem == null || currentItem.itemMeta == null) return@runBlocking
                    val currentItemMeta = currentItem.itemMeta
                    val currentItemContainer = currentItemMeta.persistentDataContainer
                    val page = currentItemContainer.get(pageSetKey, PersistentDataType.INTEGER)
                    if (page != null) {
                        val cost = currentItemContainer.getOrDefault(costKey, BooleanPDT.Companion.BOOLEAN, false)
                        val allowUnchoosable = currentItemContainer.getOrDefault(
                            displayOnlyKey,
                            BooleanPDT.Companion.BOOLEAN,
                            false
                        )
                        val scroll = currentItemContainer.get(pageScrollKey, PersistentDataType.INTEGER)
                        if (scroll == null) return@runBlocking
                        player.playSound(player.location, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f)
                        openOriginSwapper(player, getReason(item), page, scroll, cost, allowUnchoosable, layer)
                    }
                    if (currentItemContainer.has(confirmKey, BooleanPDT.Companion.BOOLEAN)) {
                        var amount = config.swapCommand.vault.defaultCost

                        if (!player.hasPermission(config.swapCommand.vault.bypassPermission) && currentItemContainer.has(
                                costsCurrencyKey, PersistentDataType.INTEGER
                            )
                        ) {
                            amount = currentItemContainer.getOrDefault(
                                costsCurrencyKey,
                                PersistentDataType.INTEGER,
                                amount
                            )
                            if (!instance.economy!!.has(player, amount.toDouble())) {
                                return@runBlocking
                            } else {
                                origins.economy!!.withdrawPlayer(player, amount.toDouble())
                            }
                        }
                        val originName = item.itemMeta.persistentDataContainer
                            .get(originKey, PersistentDataType.STRING)
                        if (originName == null) return@runBlocking
                        val origin = if (originName.equals("random", ignoreCase = true)) {
                            val excludedOrigins = config.originSelection.randomOption.exclude

                            val origins = AddonLoader.getOrigins(layer)
                            val iterator = origins.iterator()
                            while (iterator.hasNext()) {
                                val origin1 = iterator.next()
                                if (excludedOrigins.contains(origin1.getName()) || origin1.isUnchoosable(player)) {
                                    iterator.remove()
                                }
                            }
                            if (origins.isEmpty()) {
                                getFirstOrigin(layer)
                            } else {
                                origins[random.nextInt(origins.size)]
                            }
                        } else {
                            AddonLoader.getOrigin(originName)
                        }

                        val reason = getReason(item)

                        player.playSound(player.location, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f)
                        player.closeInventory()

                        if (reason == SwapReason.ORB_OF_ORIGIN) orbCooldown.put(player, System.currentTimeMillis())
                        val resetPlayer: Boolean = shouldResetPlayer(reason)
                        if (origin!!.isUnchoosable(player)) {
                            openOriginSwapper(player, reason, 0, 0, layer)
                            return@runBlocking
                        }
                        getCooldowns().setCooldown(player, OriginCommand.key)
                        setOrigin(player, origin, reason, resetPlayer, layer)
                    } else if (currentItemContainer.has(
                            closeKey,
                            BooleanPDT.Companion.BOOLEAN
                        )
                    ) event.whoClicked.closeInventory()
                }
            }
        }
    }

    @EventHandler
    fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(instance, { resetAttributes(event.getPlayer()) }, 5)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        CoroutineScope(ioDispatcher).launch {
            val player = event.player
            loadOrigins(player)
            withContext(bukkitDispatcher) {
                resetAttributes(player)
            }
            lastJoinedTick.put(player, Bukkit.getCurrentTick())
            if (player.openInventory.type == InventoryType.CHEST) return@launch
            for (layer in AddonLoader.layers) {
                val origin = getOrigin(player, layer!!)

                if (origin != null) {
                    if (origin.team == null) {
                        return@launch
                    }
                    origin.team.addPlayer(player)
                } else {
                    val defaultOrigin = getDefaultOrigin(layer)
                    if (defaultOrigin != null) {
                        setOrigin(player, defaultOrigin, SwapReason.INITIAL, false, layer)

                    } else if (OriginsReforged.mainConfig.originSelection.randomize[layer] == true) {
                        selectRandomOrigin(player, SwapReason.INITIAL, layer)
                    } else if (ShortcutUtils.isBedrockPlayer(player.uniqueId)) {
                        val delayMillis = OriginsReforged.mainConfig.geyser.joinFormDelay.toLong()
                        delay(delayMillis)
                        launch(bukkitDispatcher) {
                            GeyserSwapper.openOriginSwapper(player, SwapReason.INITIAL, false, false, layer)
                        }
                    } else {
                        launch(bukkitDispatcher) {
                            openOriginSwapper(player, SwapReason.INITIAL, 0, 0, layer)
                        }
                    }
                }
            }
        }
    }


    fun startScheduledTask() {
        CoroutineScope(ioDispatcher).launch {
            while (true) {
                updateAllPlayers()
                delay(500)
            }
        }
    }

    private suspend fun updateAllPlayers() = withContext(ioDispatcher) {
        val config = OriginsReforged.mainConfig
        val delay: Int = config.originSelection.delayBeforeRequired
        val currentTick = Bukkit.getCurrentTick()
        val disableFlightStuff = config.miscSettings.disableFlightStuff
        val originSelectionRandomize = config.originSelection.randomize

        val onlinePlayers = Bukkit.getOnlinePlayers().toList()

        for (player in onlinePlayers) {
            val lastJoinTick = lastJoinedTick.getOrPut(player) { currentTick }!!

            if (currentTick - delay < lastJoinTick) {
                continue
            }

            val reason = lastSwapReasons.getOrDefault(player, SwapReason.INITIAL)
            val shouldDisallow = shouldDisallowSelection(player, reason)

            if (shouldDisallow) {
                launch(bukkitDispatcher) {
                    player.allowFlight = AbilityRegister.canFly(player, true)
                    AbilityRegister.updateFlight(player, true)
                    resetAttributes(player)
                }
                continue
            }

            launch(bukkitDispatcher) {
                if (!disableFlightStuff) {
                    player.allowFlight = AbilityRegister.canFly(player, false)
                    AbilityRegister.updateFlight(player, false)
                }

                player.isInvisible = AbilityRegister.isInvisible(player)
                applyAttributeChanges(player)
            }

            val layer = getFirstUnselectedLayer(player) ?: continue

            val hasChestOpen = player.openInventory.type == InventoryType.CHEST
            if (hasChestOpen) continue

            val defaultOrigin = getDefaultOrigin(layer)
            if (defaultOrigin != null) {
                setOrigin(player, defaultOrigin, SwapReason.INITIAL, false, layer)
                continue
            }

            val shouldRandomize = originSelectionRandomize[layer] == true
            val isBedrockPlayer = ShortcutUtils.isBedrockPlayer(player.uniqueId)

            if (!shouldRandomize && !isBedrockPlayer) {
                launch(bukkitDispatcher) {
                    openOriginSwapper(player, reason, 0, 0, layer)
                }
            }
        }
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        CoroutineScope(ioDispatcher).launch {
            if (hasNotSelectedAllOrigins(event.player)) event.isCancelled = true
        }
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {

        val player = event.entity as? Player ?: return

        if (invulnerableMode.equals("INITIAL", ignoreCase = true) && runBlocking { hasNotSelectedAllOrigins(player) }) {
            event.isCancelled = true
            return
        }

        if (invulnerableMode.equals("ON", ignoreCase = true)) {
            player.openInventory.topInventory.getItem(1)?.itemMeta?.persistentDataContainer?.let { container ->
                if (container.has(originKey, PersistentDataType.STRING)) {
                    event.isCancelled = true
                }
            }

        }
    }

    suspend fun hasNotSelectedAllOrigins(player: Player): Boolean {
        for (layer in AddonLoader.layers) {
            if (getOrigin(player, layer!!) == null) return true
        }
        return false
    }

    @EventHandler
    fun onPlayerSwapOrigin(event: PlayerSwapOriginEvent) {
        val player = event.getPlayer()
        val newOrigin = event.newOrigin ?: return

        executeCommands("default", player)

        val originName = newOrigin.getActualName().replace(" ", "_").lowercase(Locale.getDefault())
        executeCommands(originName, player)

        if (!OriginsReforged.mainConfig.originSelection.autoSpawnTeleport) return

        if (event.reason == SwapReason.INITIAL || event.reason == SwapReason.DIED) {
            val loc = nmsInvoker.getRespawnLocation(player)
                ?: getRespawnWorld(listOf(newOrigin)).spawnLocation
            player.teleport(loc)
        }
    }

    private fun executeCommands(originName: String, player: Player) {
        val console = Bukkit.getConsoleSender()
        OriginsReforged.mainConfig.commandsOnOrigin[originName]?.forEach { command ->
            val parsedCommand = command
                .replace("%player%", player.name)
                .replace("%uuid%", player.uniqueId.toString())
            Bukkit.dispatchCommand(console, parsedCommand)
        }
    }


    private val lastRespawnReasons: MutableMap<Player?, MutableSet<PlayerRespawnEvent.RespawnFlag?>?> =
        HashMap<Player?, MutableSet<PlayerRespawnEvent.RespawnFlag?>?>()

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        if (nmsInvoker.getRespawnLocation(event.player) == null) {
            CoroutineScope(ioDispatcher).launch {
                val originRespawnWorld = getOrigins(event.getPlayer())
                launch(bukkitDispatcher) {
                    val world = getRespawnWorld(originRespawnWorld)
                    event.respawnLocation = world.spawnLocation
                }
            }
        }
        lastRespawnReasons.put(event.getPlayer(), event.respawnFlags)
    }

    @EventHandler
    fun onPlayerPostRespawn(event: PlayerPostRespawnEvent) {
        CoroutineScope(ioDispatcher).launch {
            if (lastRespawnReasons[event.player]!!.contains(PlayerRespawnEvent.RespawnFlag.END_PORTAL)) return@launch
            if (OriginsReforged.mainConfig.originSelection.deathOriginChange) {
                for (layer in AddonLoader.layers) {
                    setOrigin(event.player, null, SwapReason.DIED, false, layer!!)

                    if (OriginsReforged.mainConfig.originSelection.randomize[layer] == true) {
                        selectRandomOrigin(event.player, SwapReason.INITIAL, layer)
                    } else {
                        openOriginSwapper(event.player, SwapReason.INITIAL, 0, 0, layer)
                    }
                }
            }
        }
    }

    fun getReason(icon: ItemStack): SwapReason {
        return SwapReason.get(
            icon.itemMeta.persistentDataContainer.get(swapTypeKey, PersistentDataType.STRING)
        )
    }

    private val invulnerableMode: String = OriginsReforged.mainConfig.originSelection.invulnerableMode

    class LineData {
        class LineComponent {
            enum class LineType {
                TITLE,
                DESCRIPTION
            }

            private val component: Component
            val type: LineType?
            val rawText: String?
            val isEmpty: Boolean

            constructor(component: Component, type: LineType, rawText: String) {
                this.component = component
                this.type = type
                this.rawText = rawText
                this.isEmpty = false
            }

            constructor() {
                this.type = LineType.DESCRIPTION
                this.component = Component.empty()
                this.rawText = ""
                this.isEmpty = true
            }

            fun getComponent(lineNumber: Int): Component {
                val prefix = if (type == LineType.DESCRIPTION) "" else "title_"
                val formatted = "minecraft:${prefix}text_line_$lineNumber"
                return applyFont(component, Key.key(formatted))
            }
        }

        val rawLines: MutableList<LineComponent>

        constructor(origin: Origin) {
            this.rawLines = ArrayList<LineComponent>()
            rawLines.addAll(makeLineFor(origin.getDescription(), LineType.DESCRIPTION))
            val visibleAbilities: List<VisibleAbility> = origin.getVisibleAbilities()
            val size = visibleAbilities.size
            var count = 0
            if (size > 0) rawLines.add(LineComponent())
            for (visibleAbility in visibleAbilities) {
                count++
                rawLines.addAll(visibleAbility.title)
                rawLines.addAll(visibleAbility.description)
                if (count < size) rawLines.add(LineComponent())
            }
        }

        constructor(lines: MutableList<LineComponent>) {
            this.rawLines = lines
        }

        fun getLines(startingPoint: Int): List<Component> {
            val end = minOf(startingPoint + 6, rawLines.size)
            return (startingPoint until end).map { index ->
                rawLines[index].getComponent(index - startingPoint)
            }
        }

        companion object {
            // TODO Deprecate this and replace it with 'description' and 'title' methods inside VisibleAbility which returns the specified value as a fallback
            fun makeLineFor(text: String, type: LineType): MutableList<LineComponent> {
                val resultList = mutableListOf<LineComponent>()

                // Разбиваем текст на первую строку и остаток (если есть)
                val lines = text.split("\n", limit = 2)
                var firstLine = lines[0]
                val otherPart = StringBuilder()
                if (lines.size > 1) {
                    otherPart.append(lines[1])
                }

                // Если первая строка содержит пробелы и её ширина превышает 140,
                // разбиваем строку по словам так, чтобы первая часть не превышала ширину 140.
                // Слова, которые не помещаются, собираем в отдельную строку (overflowLine)
                if (firstLine.contains(' ') && getWidth(firstLine) > 140) {
                    val tokens = firstLine.split(" ")
                    val firstPartBuilder = StringBuilder(tokens[0])
                    var currentWidth = getWidth(firstPartBuilder.toString())
                    val spaceWidth = getWidth(" ")
                    val overflowTokens = mutableListOf<String>()

                    // Перебираем оставшиеся слова
                    for (i in 1 until tokens.size) {
                        val token = tokens[i]
                        val tokenWidth = getWidth(token)
                        if (currentWidth + spaceWidth + tokenWidth <= 140) {
                            firstPartBuilder.append(' ').append(token)
                            currentWidth += spaceWidth + tokenWidth
                        } else {
                            overflowTokens.add(token)
                        }
                    }
                    firstLine = firstPartBuilder.toString()
                    // Если есть слова, не поместившиеся в первую строку, формируем отдельную строку для переноса
                    if (overflowTokens.isNotEmpty()) {
                        val overflowLine = overflowTokens.joinToString(" ")
                        // Вставляем строку переноса в начало остального текста,
                        // чтобы она сразу шла после первой строки, а затем следовало остальное содержимое
                        otherPart.insert(0, "$overflowLine\n")
                    }
                }

                // Если тип строки DESCRIPTION, добавляем специальный символ в начало
                if (type == LineType.DESCRIPTION) {
                    firstLine = '\uF00A' + firstLine
                }

                // Форматирование строки:
                // между каждым символом вставляем символ '\uF000',
                // а в "сырую" строку (raw) добавляем символы, кроме '\uF00A'
                val formattedBuilder = StringBuilder()
                val rawBuilder = StringBuilder()
                for (char in firstLine) {
                    formattedBuilder.append(char)
                    if (char != '\uF00A') {
                        rawBuilder.append(char)
                    }
                    formattedBuilder.append('\uF000')
                }
                rawBuilder.append(' ')

                // Создаём компонент с нужным цветом
                val comp = Component.text(formattedBuilder.toString())
                    .color(
                        if (type == LineType.TITLE)
                            NamedTextColor.WHITE
                        else
                            TextColor.fromHexString("#CACACA")
                    )
                    .append(Component.text(getInverse(firstLine)))

                resultList.add(LineComponent(comp, type, rawBuilder.toString()))

                // Если осталась часть текста, обрабатываем её рекурсивно
                if (otherPart.isNotEmpty()) {
                    val trimmed = otherPart.toString().trimStart()  // убираем ведущие пробелы
                    resultList.addAll(makeLineFor(trimmed, type))
                }

                return resultList
            }

        }
    }

    class BooleanPDT : PersistentDataType<Byte, Boolean> {

        override fun getPrimitiveType() = Byte::class.javaObjectType

        override fun getComplexType() = Boolean::class.java

        override fun toPrimitive(complex: Boolean, context: PersistentDataAdapterContext) =
            if (complex) 1.toByte() else 0.toByte()

        override fun fromPrimitive(primitive: Byte, context: PersistentDataAdapterContext) =
            primitive >= 1

        companion object {
            val BOOLEAN = BooleanPDT()
        }
    }

    companion object {
        private val displayKey = NamespacedKey(instance, "displayed-item")
        private val layerKey = NamespacedKey(instance, "layer")
        private val confirmKey = NamespacedKey(instance, "confirm-select")
        private val costsCurrencyKey = NamespacedKey(instance, "costs-currency")
        private val originKey = NamespacedKey(instance, "origin-name")
        private val swapTypeKey = NamespacedKey(instance, "swap-type")
        private val pageSetKey = NamespacedKey(instance, "page-set")
        private val pageScrollKey = NamespacedKey(instance, "page-scroll")
        private val costKey = NamespacedKey(instance, "enable-cost")
        private val displayOnlyKey = NamespacedKey(instance, "display-only")
        private val closeKey = NamespacedKey(instance, "close")
        private val random = Random()

        var origins: OriginsReforged = instance
        var nmsInvoker: NMSInvoker = OriginsReforged.NMSInvoker

        fun getInverse(string: String): String {
            val result = StringBuilder()
            for (c in string.toCharArray()) {
                result.append(getInverse(c))
            }
            return result.toString()
        }

        @Deprecated("Origins-Reborn now has a 'layer' system, allowing for multiple origins to be set at once")
        fun openOriginSwapper(
            player: Player,
            reason: SwapReason,
            slot: Int,
            scrollAmount: Int,
            cost: Boolean,
            displayOnly: Boolean
        ) {
            openOriginSwapper(player, reason, slot, scrollAmount, cost, displayOnly, "origin")
        }

        @Deprecated("Origins-Reborn now has a 'layer' system, allowing for multiple origins to be set at once")
        fun openOriginSwapper(player: Player, reason: SwapReason, slot: Int, scrollAmount: Int) {
            openOriginSwapper(player, reason, slot, scrollAmount, "origin")
        }

        @Deprecated("Origins-Reborn now has a 'layer' system, allowing for multiple origins to be set at once")
        fun openOriginSwapper(player: Player, reason: SwapReason, slot: Int, scrollAmount: Int, cost: Boolean) {
            openOriginSwapper(player, reason, slot, scrollAmount, cost, "origin")
        }

        fun openOriginSwapper(player: Player, reason: SwapReason, slot: Int, scrollAmount: Int, layer: String) {
            openOriginSwapper(player, reason, slot, scrollAmount, false, displayOnly = false, layer = layer)
        }

        fun openOriginSwapper(
            player: Player,
            reason: SwapReason,
            slot: Int,
            scrollAmount: Int,
            cost: Boolean,
            layer: String
        ) {
            openOriginSwapper(player, reason, slot, scrollAmount, cost, false, layer)
        }

        fun openOriginSwapper(
            player: Player,
            reason: SwapReason,
            slot: Int,
            scrollAmount: Int,
            cost: Boolean,
            displayOnly: Boolean,
            layer: String
        ) {
            val config = OriginsReforged.mainConfig
            var slotIndex = slot

            if (shouldDisallowSelection(player, reason)) return

            if (reason == SwapReason.INITIAL) {
                config.originSelection.defaultOrigin.values.first().let { def ->
                    getOriginByFilename(def)?.let { defaultOrigin ->
                        CoroutineScope(ioDispatcher).launch {
                            setOrigin(player, defaultOrigin, reason, false, layer)
                        }
                        return
                    }
                }
            }

            lastSwapReasons[player] = reason
            val enableRandom = config.originSelection.randomOption.enabled

            val checkBedrockSwap = runBlocking {
                !GeyserSwapper.checkBedrockSwap(player, reason, cost, displayOnly, layer)
            }
            if (checkBedrockSwap) return
            val allOrigins = AddonLoader.getOrigins(layer)
            if (allOrigins.isEmpty()) return

            val origins = allOrigins.toMutableList()
            if (!displayOnly) {
                val iterator = origins.iterator()
                while (iterator.hasNext()) {
                    val origin = iterator.next()
                    val isUnchoosable = runBlocking {
                        origin.isUnchoosable(player)
                    }
                    if (isUnchoosable || (origin.hasPermission() && !player.hasPermission(origin.permission!!))) {
                        iterator.remove()
                    }
                }
            }

            // Нормализуем индекс слота
            while (slotIndex > origins.size || (slotIndex == origins.size && !enableRandom)) {
                slotIndex -= origins.size + if (enableRandom) 1 else 0
            }
            while (slotIndex < 0) {
                slotIndex += origins.size + if (enableRandom) 1 else 0
            }

            val icon: ItemStack
            val name: String
            val nameForDisplay: String
            val impact: Char
            var costAmount = config.swapCommand.vault.defaultCost
            val data: LineData

            if (slotIndex == origins.size) {
                val excludedOriginNames = config.originSelection.randomOption.exclude.map { s ->
                    getOriginByFilename(s)?.getName()
                }
                icon = OrbOfOrigin.orb.clone()
                name = "Random"
                nameForDisplay = "Random"
                impact = '\uE002'

                val descriptionText = StringBuilder(
                    "You'll be assigned one of the following:\n\n"
                )
                origins.forEach { origin ->
                    if (!excludedOriginNames.contains(origin.getName())) {
                        descriptionText.append(origin.getName()).append("\n")
                    }
                }
                data = LineData(LineData.makeLineFor(descriptionText.toString(), LineType.DESCRIPTION))
            } else {
                val origin = origins[slotIndex]
                icon = origin.icon
                name = origin.getName()
                nameForDisplay = origin.getNameForDisplay()
                impact = origin.impact
                data = LineData(origin)
                origin.cost?.let { costAmount = it }
            }

            val compressedName = buildString {
                append("\uF001")
                nameForDisplay.forEach { c ->
                    append(c)
                    append('\uF000')
                }
            }

            val background = applyFont(
                ShortcutUtils.getColored(config.originSelection.screenTitle.background),
                Key.key("minecraft:default")
            )
            var component = applyFont(
                Component.text("\uF000\uE000\uF001\uE001\uF002$impact"),
                Key.key("minecraft:origin_selector")
            )
                .color(NamedTextColor.WHITE)
                .append(background)
                .append(
                    applyFont(Component.text(compressedName), Key.key("minecraft:origin_title_text")).color(
                        NamedTextColor.WHITE
                    )
                )
                .append(
                    applyFont(
                        Component.text(getInverse(nameForDisplay) + "\uF000"),
                        Key.key("minecraft:reverse_text")
                    ).color(NamedTextColor.WHITE)
                )
            data.getLines(scrollAmount).forEach { line ->
                component = component.append(line)
            }

            val prefix = applyFont(
                ShortcutUtils.getColored(config.originSelection.screenTitle.prefix),
                Key.key("minecraft:default")
            )
            val suffix = applyFont(
                ShortcutUtils.getColored(config.originSelection.screenTitle.suffix),
                Key.key("minecraft:default")
            )

            val invHolder = CustomGuiFactory(
                CustomGuiFactory.CustomInventoryType.ORIGINS_SWAPPER,
                54,
                prefix.append(component).append(suffix)
            )
            val swapperInventory = invHolder.inventory

            // Настраиваем метаданные и сохраняем данные в persistentDataContainer
            val meta = icon.itemMeta
            val container = meta.persistentDataContainer
            container.set(originKey, PersistentDataType.STRING, name.lowercase(Locale.getDefault()))
            if (meta is SkullMeta) {
                meta.owningPlayer = player
            }
            container.set(displayKey, BooleanPDT.BOOLEAN, true)
            container.set(swapTypeKey, PersistentDataType.STRING, reason.reason)
            container.set(layerKey, PersistentDataType.STRING, layer)
            icon.itemMeta = meta
            swapperInventory.setItem(1, icon)

            // Создаём предметы для подтверждения и «невидимого» подтверждения
            val confirm = ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
            val invisibleConfirm = ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
            var confirmMeta = confirm.itemMeta
            val confirmContainer = confirmMeta.persistentDataContainer
            var invisibleConfirmMeta = invisibleConfirm.itemMeta
            val invisibleConfirmContainer = invisibleConfirmMeta.persistentDataContainer

            confirmMeta.displayName(
                Component.text("Confirm")
                    .color(NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)
            )
            confirmMeta = nmsInvoker.setCustomModelData(confirmMeta, 5)
            if (!displayOnly) {
                confirmContainer.set(confirmKey, BooleanPDT.BOOLEAN, true)
            } else {
                confirmContainer.set(closeKey, BooleanPDT.BOOLEAN, true)
            }

            invisibleConfirmMeta.displayName(
                Component.text("Confirm")
                    .color(NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)
            )
            invisibleConfirmMeta = nmsInvoker.setCustomModelData(invisibleConfirmMeta, 6)
            if (!displayOnly) {
                invisibleConfirmContainer.set(confirmKey, BooleanPDT.BOOLEAN, true)
            } else {
                invisibleConfirmContainer.set(closeKey, BooleanPDT.BOOLEAN, true)
            }

            // Если стоит опция стоимости – добавляем описание и сохраняем стоимость в метаданных
            if (costAmount != 0 && cost && !player.hasPermission(config.swapCommand.vault.bypassPermission)) {
                val vaultConfig = config.swapCommand.vault
                CoroutineScope(ioDispatcher).launch {
                    val canPurchase =
                        !vaultConfig.permanentPurchases || !DatabaseManager.getUsedOrigins(player.uniqueId.toString())
                            .contains(name)
                    if (canPurchase) {
                        val symbol = vaultConfig.currencySymbol
                        val hasFunds = instance.economy!!.has(player, costAmount.toDouble())
                        val costMessage = if (hasFunds)
                            "This will cost $symbol$costAmount of your balance!"
                        else
                            "You need at least %s%s in your balance to do this!"
                        val costLore = listOf(Component.text(costMessage))
                        withContext(bukkitDispatcher) {
                            confirmMeta.lore(costLore)
                            invisibleConfirmMeta.lore(costLore)
                            confirmContainer.set(costsCurrencyKey, PersistentDataType.INTEGER, costAmount)
                            invisibleConfirmContainer.set(costsCurrencyKey, PersistentDataType.INTEGER, costAmount)
                        }
                    }
                }
            }

            // Настраиваем кнопки прокрутки (Up и Down)
            val up = ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
            val down = ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
            var upMeta = up.itemMeta
            val upContainer = upMeta.persistentDataContainer
            var downMeta = down.itemMeta
            val downContainer = downMeta.persistentDataContainer

            val scrollSize = config.originSelection.scrollAmount
            upMeta.displayName(
                Component.text("Up")
                    .color(NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)
            )
            if (scrollAmount != 0) {
                upContainer.set(pageSetKey, PersistentDataType.INTEGER, slotIndex)
                upContainer.set(pageScrollKey, PersistentDataType.INTEGER, max(scrollAmount - scrollSize, 0))
            }
            upMeta = nmsInvoker.setCustomModelData(upMeta, 3 + if (scrollAmount == 0) 6 else 0)
            upContainer.set(costKey, BooleanPDT.BOOLEAN, cost)
            upContainer.set(displayOnlyKey, BooleanPDT.BOOLEAN, displayOnly)

            val remainingSize = data.rawLines.size - scrollAmount - 6
            val canGoDown = remainingSize > 0
            downMeta.displayName(
                Component.text("Down")
                    .color(NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)
            )
            if (canGoDown) {
                downContainer.set(pageSetKey, PersistentDataType.INTEGER, slotIndex)
                downContainer.set(
                    pageScrollKey,
                    PersistentDataType.INTEGER,
                    min(scrollAmount + scrollSize, scrollAmount + remainingSize)
                )
            }
            downMeta = nmsInvoker.setCustomModelData(downMeta, 4 + if (!canGoDown) 6 else 0)
            downContainer.set(costKey, BooleanPDT.BOOLEAN, cost)
            downContainer.set(displayOnlyKey, BooleanPDT.BOOLEAN, displayOnly)

            up.itemMeta = upMeta
            down.itemMeta = downMeta
            swapperInventory.setItem(52, up)
            swapperInventory.setItem(53, down)

            // Если не только для отображения, добавляем навигационные кнопки (предыдущая/следующая опция)
            if (!displayOnly) {
                val left = ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                val right = ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                var leftMeta = left.itemMeta
                val leftContainer = leftMeta.persistentDataContainer
                var rightMeta = right.itemMeta
                val rightContainer = rightMeta.persistentDataContainer

                leftMeta.displayName(
                    Component.text("Previous origin")
                        .color(NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false)
                )
                leftContainer.set(pageSetKey, PersistentDataType.INTEGER, slotIndex - 1)
                leftContainer.set(pageScrollKey, PersistentDataType.INTEGER, 0)
                leftMeta = nmsInvoker.setCustomModelData(leftMeta, 1)
                leftContainer.set(costKey, BooleanPDT.BOOLEAN, cost)
                leftContainer.set(displayOnlyKey, BooleanPDT.BOOLEAN, false)

                rightMeta.displayName(
                    Component.text("Next origin")
                        .color(NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false)
                )
                rightContainer.set(pageSetKey, PersistentDataType.INTEGER, slotIndex + 1)
                rightContainer.set(pageScrollKey, PersistentDataType.INTEGER, 0)
                rightMeta = nmsInvoker.setCustomModelData(rightMeta, 2)
                rightContainer.set(costKey, BooleanPDT.BOOLEAN, cost)
                rightContainer.set(displayOnlyKey, BooleanPDT.BOOLEAN, false)

                left.itemMeta = leftMeta
                right.itemMeta = rightMeta
                swapperInventory.setItem(47, left)
                swapperInventory.setItem(51, right)
            }

            confirm.itemMeta = confirmMeta
            invisibleConfirm.itemMeta = invisibleConfirmMeta
            swapperInventory.setItem(48, confirm)
            swapperInventory.setItem(49, invisibleConfirm)
            swapperInventory.setItem(50, invisibleConfirm)

            player.openInventory(swapperInventory)
        }


        fun applyFont(component: Component, font: Key): Component {
            return component.font(font)
        }

        fun shouldResetPlayer(reason: SwapReason): Boolean {
            return when (reason) {
                SwapReason.COMMAND -> OriginsReforged.mainConfig.swapCommand.resetPlayer
                SwapReason.ORB_OF_ORIGIN -> OriginsReforged.mainConfig.orbOfOrigin.resetPlayer
                else -> false
            }
        }

        fun getWidth(s: String): Int {
            return s.sumOf { WidthGetter.getWidth(it) }
        }

        fun getInverse(c: Char): String {
            val sex = when (WidthGetter.getWidth(c)) {
                0 -> ""
                2 -> "\uF001"
                3 -> "\uF002"
                4 -> "\uF003"
                5 -> "\uF004"
                6 -> "\uF005"
                7 -> "\uF006"
                8 -> "\uF007"
                9 -> "\uF008"
                10 -> "\uF009"
                11 -> "\uF008\uF001"
                12 -> "\uF009\uF001"
                13 -> "\uF009\uF002"
                14 -> "\uF009\uF003"
                15 -> "\uF009\uF004"
                16 -> "\uF009\uF005"
                17 -> "\uF009\uF006"
                else -> throw IllegalStateException("Unexpected value for character: $c")
            }
            return sex
        }

        @JvmField
        var orbCooldown: MutableMap<Player?, Long?> = HashMap<Player?, Long?>()

        fun resetPlayer(player: Player, fullReset: Boolean) {
            resetAttributes(player)
            player.closeInventory()
            nmsInvoker.setWorldBorderOverlay(player, false)
            player.setCooldown(Material.SHIELD, 0)
            player.allowFlight = false
            player.isFlying = false
            for (otherPlayer in Bukkit.getOnlinePlayers()) {
                AbilityRegister.updateEntity(player, otherPlayer)
            }
            for (effect in player.activePotionEffects) {
                if (effect.amplifier == -1 || ShortcutUtils.isInfinite(effect)) player.removePotionEffect(effect.type)
            }
            if (!fullReset) return
            player.inventory.clear()
            player.enderChest.clear()
            player.saturation = 5f
            player.fallDistance = 0f
            player.remainingAir = player.maximumAir
            player.foodLevel = 20
            player.fireTicks = 0
            player.health = getMaxHealth(player)
            for (effect in player.activePotionEffects) {
                player.removePotionEffect(effect.type)
            }
            CoroutineScope(ioDispatcher).launch {
                val world: World = getRespawnWorld(getOriginsSync(player))
                withContext(bukkitDispatcher) {
                    player.teleport(world.spawnLocation)
                    nmsInvoker.resetRespawnLocation(player)
                }
            }
        }

        fun getRespawnWorld(origin: List<Origin>): World {

            origin.flatMap { it.getAbilities() }
                .filterIsInstance<DefaultSpawnAbility>()
                .firstNotNullOfOrNull { it.world }
                ?.let { return it }

            val overworld = OriginsReforged.mainConfig.worlds.world

            return Bukkit.getWorld(overworld) ?: Bukkit.getWorlds()[0]
        }


        fun getMaxHealth(player: Player): Double {
            applyAttributeChanges(player)
            val instance = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)
            if (instance == null) return 20.0
            return instance.value
        }

        fun applyAttributeChanges(player: Player) {
            AbilityRegister.abilityMap.values.filterIsInstance<AttributeModifierAbility>().forEach { ability ->
                val instance = runCatching { player.getAttribute(ability.attribute) }.getOrNull() ?: return@forEach

                val abilityKeyStr = ability.key.asString()
                val key = NamespacedKey(origins, abilityKeyStr.replace(":", "-"))

                val requiredAmount = ability.getTotalAmount(player)
                val hasAbility = ability.hasAbility(player)
                val currentModifier = nmsInvoker.getAttributeModifier(instance, key)

                if (hasAbility) {
                    if (currentModifier?.amount != requiredAmount) {
                        currentModifier?.let { instance.removeModifier(it) }
                        nmsInvoker.addAttributeModifier(
                            instance,
                            key,
                            abilityKeyStr,
                            requiredAmount,
                            ability.actualOperation
                        )
                    }
                } else {
                    currentModifier?.let { instance.removeModifier(it) }
                }
            }
        }


        fun resetAttributes(player: Player) {
            val initialHealth = player.health
            Attribute.values().forEach { attribute ->
                player.getAttribute(attribute)?.let { instance ->
                    instance.modifiers.toList().forEach { modifier ->
                        instance.removeModifier(modifier)
                    }
                }
            }

            Bukkit.getScheduler().scheduleSyncDelayedTask(origins, {
                player.getAttribute(nmsInvoker.maxHealthAttribute)?.let { mh ->
                    player.health = min(mh.value, initialHealth)
                }
            }, 10)
        }


        private val lastSwapReasons: MutableMap<Player?, SwapReason> = HashMap<Player?, SwapReason>()

        private val lastJoinedTick: MutableMap<Player?, Int?> = HashMap<Player?, Int?>()


        fun shouldDisallowSelection(player: Player, reason: SwapReason): Boolean {
            runCatching { AuthMeApi.getInstance().isAuthenticated(player) }
                .getOrNull()?.let { return !it }
            val worldId = player.world.name
            return !shouldOpenSwapMenu(player, reason) || OriginsReforged.mainConfig.worlds.disabledWorlds.contains(
                worldId
            )
        }


        suspend fun selectRandomOrigin(player: Player, reason: SwapReason, layer: String) {
            val origin = getRandomOrigin(layer)
            setOrigin(player, origin, reason, shouldResetPlayer(reason), layer)
            openOriginSwapper(player, reason, AddonLoader.getOrigins(layer).indexOf(origin), 0, false, true, layer)
        }

        @Deprecated("Origins-Reborn now has a 'layer' system, allowing for multiple origins to be set at once")
        suspend fun getOrigin(player: Player): Origin? {
            return getOrigin(player, "origin")
        }

        suspend fun getOrigin(player: Player, layer: String): Origin? {
            return if (player.persistentDataContainer.has(originKey, PersistentDataType.STRING)) {
                getStoredOrigin(player, layer)
            } else {
                player.persistentDataContainer.get(originKey, PersistentDataType.TAG_CONTAINER)
                    ?.get(AddonLoader.layerKeys[layer]!!, PersistentDataType.STRING)
                    ?.let { name ->
                        AddonLoader.getOrigin(name)
                    }
            }
        }

        suspend fun getStoredOrigin(player: Player, layer: String): Origin? {
            val playerId = player.uniqueId.toString()

            val originName = DatabaseManager.getOriginForLayer(playerId, layer) ?: "null"
            return AddonLoader.getOrigin(originName)
        }


        suspend fun loadOrigins(player: Player) {
            withContext(bukkitDispatcher) {
                player.persistentDataContainer.remove(originKey)
            }

            AddonLoader.layers.filterNotNull().forEach { layer ->
                getStoredOrigin(player, layer)?.let { origin ->
                    withContext(bukkitDispatcher) {
                        val pdc = player.persistentDataContainer.get(originKey, PersistentDataType.TAG_CONTAINER)
                            ?: player.persistentDataContainer.adapterContext.newPersistentDataContainer()

                        pdc.set(
                            AddonLoader.layerKeys[layer]!!,
                            PersistentDataType.STRING,
                            origin.getName().lowercase(Locale.getDefault())
                        )

                        player.persistentDataContainer.set(originKey, PersistentDataType.TAG_CONTAINER, pdc)
                    }
                }
            }
        }

        fun getOriginsSync(player: Player): List<Origin> {
            val container = player.persistentDataContainer.get(originKey, PersistentDataType.TAG_CONTAINER)
                ?: return emptyList()

            return AddonLoader.layers.filterNotNull().mapNotNull { layer ->
                container.get(AddonLoader.layerKeys[layer]!!, PersistentDataType.STRING)?.let { name ->
                    AddonLoader.getOrigin(name)
                }
            }
        }

        suspend fun getOrigins(player: Player): MutableList<Origin> =
            AddonLoader.layers.filterNotNull()
                .mapNotNull { getOrigin(player, it) }
                .toMutableList()

        @Deprecated("Origins-Reborn now has a 'layer' system, allowing for multiple origins to be set at once")
        suspend fun setOrigin(player: Player, origin: Origin?, reason: SwapReason?, resetPlayer: Boolean) {
            setOrigin(player, origin, reason, resetPlayer, "origin")
        }

        suspend fun setOrigin(
            player: Player,
            origin: Origin?,
            reason: SwapReason?,
            resetPlayer: Boolean,
            layer: String
        ) {
            val uuid = player.uniqueId.toString()
            val swapOriginEvent = PlayerSwapOriginEvent(player, reason, resetPlayer, getOrigin(player, layer), origin)

            val event = withContext(bukkitDispatcher) {
                swapOriginEvent.callEvent()
            }
            if (!event) return

            val newOrigin = swapOriginEvent.newOrigin


            if (newOrigin == null) {
                DatabaseManager.updateOrigin(uuid, layer, null)
                withContext(bukkitDispatcher) {
                    resetPlayer(player, swapOriginEvent.isResetPlayer)
                }
                return
            }

            withContext(bukkitDispatcher) {
                newOrigin.team?.addPlayer(player)
                getCooldowns().resetCooldowns(player)
            }

            val lowerName = newOrigin.getName().lowercase(Locale.getDefault())
            DatabaseManager.updateOrigin(uuid, layer, lowerName)
            DatabaseManager.addOriginToHistory(uuid, lowerName)

            withContext(bukkitDispatcher) {
                resetPlayer(player, swapOriginEvent.isResetPlayer)
                loadOrigins(player)
            }
        }
    }
}
