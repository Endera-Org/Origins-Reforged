package ru.turbovadim.ui

import com.noxcrew.interfaces.InterfacesListeners
import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.properties.interfaceProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.AddonLoader
import ru.turbovadim.OrbOfOrigin
import ru.turbovadim.Origin
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginSwapper.Companion.applyFont
import ru.turbovadim.OriginSwapper.Companion.getInverse
import ru.turbovadim.OriginSwapper.Companion.setOrigin
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.OriginsReforged
import ru.turbovadim.OriginsReforged.Companion.bukkitDispatcher
import ru.turbovadim.OriginsReforged.Companion.getCooldowns
import ru.turbovadim.OriginsReforged.Companion.instance
import ru.turbovadim.ShortcutUtils
import ru.turbovadim.commands.OriginCommand
import ru.turbovadim.database.DatabaseManager
import ru.turbovadim.events.PlayerSwapOriginEvent.SwapReason
import java.util.*
import kotlin.math.max
import kotlin.math.min

object OriginSwapperInterface {

    private val nmsInvoker = OriginsReforged.NMSInvoker

    suspend fun open(
        player: Player,
        reason: SwapReason,
        initialPage: Int = 0,
        initialScroll: Int = 0,
        cost: Boolean = false,
        displayOnly: Boolean = false,
        layer: String = "origin"
    ) {
        val config = OriginsReforged.mainConfig

        if (OriginSwapper.shouldDisallowSelection(player, reason)) return

        // Check for default origin on initial swap
        if (reason == SwapReason.INITIAL) {
            config.originSelection.defaultOrigin.values.firstOrNull()?.let { def ->
                AddonLoader.getOriginByFilename(def)?.let { defaultOrigin ->
                    setOrigin(player, defaultOrigin, reason, false, layer)
                    return
                }
            }
        }

        // Check for Bedrock player
        val checkBedrockSwap = !ru.turbovadim.geysermc.GeyserSwapper.checkBedrockSwap(player, reason, cost, displayOnly, layer)
        if (checkBedrockSwap) return

        // Get available origins
        val allOrigins = AddonLoader.getOrigins(layer)
        if (allOrigins.isEmpty()) return

        val origins = if (displayOnly) {
            allOrigins.toMutableList()
        } else {
            allOrigins.filter { origin ->
                val isUnchoosable = origin.isUnchoosable(player)
                !isUnchoosable && (!origin.hasPermission() || player.hasPermission(origin.permission!!))
            }.toMutableList()
        }

        val enableRandom = config.originSelection.randomOption.enabled

        val originInterface = buildChestInterface {
            rows = 6

            val pageProperty = interfaceProperty(normalizePageIndex(initialPage, origins.size, enableRandom))
            val scrollProperty = interfaceProperty(initialScroll)

            // Prevent closing when origin must be selected
            addCloseHandler(
                reasons = listOf(
                    InventoryCloseEvent.Reason.PLAYER,
                    InventoryCloseEvent.Reason.DISCONNECT,
                    InventoryCloseEvent.Reason.DEATH,
                    InventoryCloseEvent.Reason.TELEPORT
                )
            ) { _, view ->
                val hasNotSelected = runBlocking { hasNotSelectedAllOrigins(player) }
                val shouldPreventClose = reason == SwapReason.ORB_OF_ORIGIN || hasNotSelected

                if (shouldPreventClose && player.isOnline) {
                    view.open()
                }
            }

            withTransform(pageProperty, scrollProperty) { pane, view ->
                var page by pageProperty
                var scroll by scrollProperty

                // Get current origin data
                val (icon, name, nameForDisplay, impact, data, originCost) = getOriginData(
                    page, origins, config, player, layer
                )

                // Build and set title
                val title = buildTitle(nameForDisplay, impact, data, scroll, config)
                view.title(title)

                // Place origin icon at slot 1 (row 0, col 1)
                val iconWithMeta = icon.clone().apply {
                    if (itemMeta is SkullMeta) {
                        val meta = itemMeta as SkullMeta
                        meta.owningPlayer = player
                        itemMeta = meta
                    }
                }
                pane[0, 1] = StaticElement(drawable(iconWithMeta))

                // Navigation buttons (only if not display-only)
                if (!displayOnly) {
                    // Previous origin button (slot 47 = row 5, col 2)
                    val leftArrow = createNavigationItem("Previous origin", 1)
                    pane[5, 2] = StaticElement(drawable(leftArrow)) {
                        player.playSound(player.location, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f)
                        page = normalizePageIndex(page - 1, origins.size, enableRandom)
                        scroll = 0
                    }

                    // Next origin button (slot 51 = row 5, col 6)
                    val rightArrow = createNavigationItem("Next origin", 2)
                    pane[5, 6] = StaticElement(drawable(rightArrow)) {
                        player.playSound(player.location, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f)
                        page = normalizePageIndex(page + 1, origins.size, enableRandom)
                        scroll = 0
                    }
                }

                // Scroll buttons
                val scrollSize = config.originSelection.scrollAmount
                val canScrollUp = scroll > 0
                val remainingSize = data.rawLines.size - scroll - 6
                val canScrollDown = remainingSize > 0

                // Up button (slot 52 = row 5, col 7)
                val upArrow = createScrollItem("Up", 3, !canScrollUp)
                pane[5, 7] = StaticElement(drawable(upArrow)) {
                    if (canScrollUp) {
                        player.playSound(player.location, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f)
                        scroll = max(scroll - scrollSize, 0)
                    }
                }

                // Down button (slot 53 = row 5, col 8)
                val downArrow = createScrollItem("Down", 4, !canScrollDown)
                pane[5, 8] = StaticElement(drawable(downArrow)) {
                    if (canScrollDown) {
                        player.playSound(player.location, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f)
                        scroll = min(scroll + scrollSize, scroll + remainingSize)
                    }
                }

                // Confirm buttons (slots 48, 49, 50 = row 5, cols 3, 4, 5)
                val confirmItem = createConfirmItem(displayOnly, originCost)
                val invisibleConfirmItem = createConfirmItem(displayOnly, originCost, invisible = true)

                val confirmHandler: suspend () -> Unit = {
                    if (displayOnly) {
                        withContext(bukkitDispatcher) {
                            InterfacesListeners.INSTANCE.getOpenPlayerInterface(player.uniqueId)?.close()
                        }
                    } else {
                        handleConfirmation(player, name, origins, reason, cost, originCost, layer, config)
                    }
                }

                pane[5, 3] = StaticElement(drawable(confirmItem)) {
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f)
                    completingLater = true
                    CoroutineScope(ioDispatcher).launch {
                        confirmHandler()
                        complete()
                    }
                }

                pane[5, 4] = StaticElement(drawable(invisibleConfirmItem)) {
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f)
                    completingLater = true
                    CoroutineScope(ioDispatcher).launch {
                        confirmHandler()
                        complete()
                    }
                }

                pane[5, 5] = StaticElement(drawable(invisibleConfirmItem)) {
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f)
                    completingLater = true
                    CoroutineScope(ioDispatcher).launch {
                        confirmHandler()
                        complete()
                    }
                }
            }
        }

        originInterface.open(player)
    }

    private data class OriginData(
        val icon: ItemStack,
        val name: String,
        val nameForDisplay: String,
        val impact: Char,
        val data: LineData,
        val cost: Int
    )

    private fun getOriginData(
        page: Int,
        origins: List<Origin>,
        config: ru.turbovadim.config.MainConfig,
        player: Player,
        layer: String
    ): OriginData {
        val defaultCost = config.swapCommand.vault.defaultCost

        return if (page >= origins.size) {
            // Random option
            val excludedOriginNames = config.originSelection.randomOption.exclude.map { s ->
                AddonLoader.getOriginByFilename(s)?.getName()
            }

            val descriptionText = StringBuilder("You'll be assigned one of the following:\n\n")
            origins.forEach { origin ->
                if (!excludedOriginNames.contains(origin.getName())) {
                    descriptionText.append(origin.getName()).append("\n")
                }
            }

            OriginData(
                icon = OrbOfOrigin.orb.clone(),
                name = "Random",
                nameForDisplay = "Random",
                impact = '\uE002',
                data = LineData(LineData.makeLineFor(descriptionText.toString(), LineType.DESCRIPTION)),
                cost = defaultCost
            )
        } else {
            val origin = origins[page]
            OriginData(
                icon = origin.icon,
                name = origin.getName(),
                nameForDisplay = origin.getNameForDisplay(),
                impact = origin.impact,
                data = LineData(origin),
                cost = origin.cost ?: defaultCost
            )
        }
    }

    private fun buildTitle(
        nameForDisplay: String,
        impact: Char,
        data: LineData,
        scrollAmount: Int,
        config: ru.turbovadim.config.MainConfig
    ): Component {
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
                applyFont(Component.text(compressedName), Key.key("minecraft:origin_title_text"))
                    .color(NamedTextColor.WHITE)
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

        return prefix.append(component).append(suffix)
    }

    private fun createNavigationItem(name: String, customModelData: Int): ItemStack {
        val item = ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
        val meta = item.itemMeta
        meta.displayName(
            Component.text(name)
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
        )
        item.itemMeta = nmsInvoker.setCustomModelData(meta, customModelData)
        return item
    }

    private fun createScrollItem(name: String, baseCustomModelData: Int, disabled: Boolean): ItemStack {
        val item = ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
        val meta = item.itemMeta
        meta.displayName(
            Component.text(name)
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
        )
        val cmd = if (disabled) baseCustomModelData + 6 else baseCustomModelData
        item.itemMeta = nmsInvoker.setCustomModelData(meta, cmd)
        return item
    }

    private fun createConfirmItem(
        displayOnly: Boolean,
        costAmount: Int,
        invisible: Boolean = false
    ): ItemStack {
        val item = ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
        var meta = item.itemMeta

        meta.displayName(
            Component.text(if (displayOnly) "Close" else "Confirm")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
        )

        meta = nmsInvoker.setCustomModelData(meta, if (invisible) 6 else 5)

        // Note: Cost lore will be added asynchronously after creation if needed
        item.itemMeta = meta
        return item
    }


    private suspend fun handleConfirmation(
        player: Player,
        originName: String,
        origins: List<Origin>,
        reason: SwapReason,
        cost: Boolean,
        costAmount: Int,
        layer: String,
        config: ru.turbovadim.config.MainConfig
    ) {
        if (costAmount != 0 && cost && !player.hasPermission(config.swapCommand.vault.bypassPermission)) {
            if (instance.economy?.has(player, costAmount.toDouble()) != true) {
                return
            }
            instance.economy?.withdrawPlayer(player, costAmount.toDouble())
        }

        val origin = if (originName.equals("random", ignoreCase = true)) {
            val excludedOrigins = config.originSelection.randomOption.exclude
            val availableOrigins = origins.filter { o ->
                !excludedOrigins.contains(o.getName()) && !o.isUnchoosable(player)
            }
            if (availableOrigins.isEmpty()) {
                AddonLoader.getFirstOrigin(layer)
            } else {
                availableOrigins.random()
            }
        } else {
            AddonLoader.getOrigin(originName.lowercase())
        }

        if (origin?.isUnchoosable(player) == true) {
            open(player, reason, 0, 0, cost, false, layer)
            return
        }

        if (reason == SwapReason.ORB_OF_ORIGIN) {
            OriginSwapper.orbCooldown[player] = System.currentTimeMillis()
        }

        val resetPlayer = OriginSwapper.shouldResetPlayer(reason)

        getCooldowns().setCooldown(player, OriginCommand.key)
        setOrigin(player, origin, reason, resetPlayer, layer)

        withContext(bukkitDispatcher) {
            InterfacesListeners.INSTANCE.getOpenPlayerInterface(player.uniqueId)?.close()
        }
    }

    private fun normalizePageIndex(index: Int, originsSize: Int, enableRandom: Boolean): Int {
        val maxIndex = originsSize + if (enableRandom) 1 else 0
        if (maxIndex == 0) return 0

        var normalized = index
        while (normalized >= maxIndex) {
            normalized -= maxIndex
        }
        while (normalized < 0) {
            normalized += maxIndex
        }
        return normalized
    }

    private suspend fun hasNotSelectedAllOrigins(player: Player): Boolean {
        for (layer in AddonLoader.layers) {
            if (OriginSwapper.getOrigin(player, layer!!) == null) return true
        }
        return false
    }
}
