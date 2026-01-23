package ru.turbovadim.v2.ui

import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.properties.interfaceProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import ru.turbovadim.OrbOfOrigin
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.OriginsReforged.Companion.v2Container
import ru.turbovadim.OriginsReforged.Companion.mainConfig
import ru.turbovadim.OriginsReforged.Companion.bukkitDispatcher
import ru.turbovadim.ShortcutUtils
import ru.turbovadim.ui.TextRenderingUtils
import ru.turbovadim.config.MainConfig
import ru.turbovadim.v2.origin.Origin
import kotlin.math.max
import kotlin.math.min

/**
 * Origin selection UI using noxcrew interfaces library.
 * Uses custom font rendering for texture pack compatibility.
 */
object OriginSelectorUI {

    /**
     * Open the origin selection UI for a player.
     *
     * @param player The player to show the UI to
     * @param layer The layer to select an origin for (default: "origin")
     * @param consumeOrb Whether to consume an orb after selection
     * @param orbSlot The slot containing the orb to consume (-1 if no orb)
     * @param initialPage Starting page index
     * @param initialScroll Starting scroll position
     * @param displayOnly If true, shows info only without selection
     */
    suspend fun open(
        player: Player,
        layer: String = "origin",
        consumeOrb: Boolean = false,
        orbSlot: Int = -1,
        initialPage: Int = 0,
        initialScroll: Int = 0,
        displayOnly: Boolean = false
    ) {
        val container = v2Container ?: return
        val config = mainConfig

        val origins = if (displayOnly) {
            container.originRegistry.getByLayer(layer).toMutableList()
        } else {
            container.originRegistry.getChoosableByLayer(layer).filter { origin ->
                !origin.requiresPermission || player.hasPermission(origin.permission!!)
            }.toMutableList()
        }

        if (origins.isEmpty()) {
            player.sendMessage(Component.text("No origins available.", NamedTextColor.RED))
            return
        }

        val enableRandom = config.originSelection.randomOption.enabled

        val ui = buildChestInterface {
            rows = 6
            onlyCancelItemInteraction = false
            prioritiseBlockInteractions = false

            val pageProperty = interfaceProperty(normalizePageIndex(initialPage, origins.size, enableRandom))
            val scrollProperty = interfaceProperty(initialScroll)

            // Set initial title
            val initialOriginData = getOriginData(
                normalizePageIndex(initialPage, origins.size, enableRandom),
                origins, config, player
            )
            titleSupplier = {
                buildTitle(
                    initialOriginData.nameForDisplay,
                    initialOriginData.impact,
                    initialOriginData.data,
                    initialScroll,
                    config
                )
            }

            withTransform(pageProperty, scrollProperty) { pane, view ->
                var page by pageProperty
                var scroll by scrollProperty

                // Get current origin data
                val (icon, name, nameForDisplay, impact, data, originCost) = getOriginData(
                    page, origins, config, player
                )

                // Build and set title
                val title = buildTitle(nameForDisplay, impact, data, scroll, config)
                view.title(title)

                // Place origin icon at slot (row 0, col 1)
                pane[0, 1] = StaticElement(drawable(icon.clone()))

                // Navigation buttons (only if not display-only)
                if (!displayOnly) {
                    // Previous origin button (row 5, col 2)
                    val leftArrow = createNavigationItem("Previous origin", 1)
                    pane[5, 2] = StaticElement(drawable(leftArrow)) {
                        player.playSound(player.location, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f)
                        page = normalizePageIndex(page - 1, origins.size, enableRandom)
                        scroll = 0
                    }

                    // Next origin button (row 5, col 6)
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
                val remainingSize = data.size - scroll - 6
                val canScrollDown = remainingSize > 0

                // Up button (row 5, col 7)
                val upArrow = createScrollItem("Up", 3, !canScrollUp)
                pane[5, 7] = StaticElement(drawable(upArrow)) {
                    if (canScrollUp) {
                        player.playSound(player.location, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f)
                        scroll = max(scroll - scrollSize, 0)
                    }
                }

                // Down button (row 5, col 8)
                val downArrow = createScrollItem("Down", 4, !canScrollDown)
                pane[5, 8] = StaticElement(drawable(downArrow)) {
                    if (canScrollDown) {
                        player.playSound(player.location, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f)
                        scroll = min(scroll + scrollSize, scroll + remainingSize)
                    }
                }

                // Confirm buttons (row 5, cols 3, 4, 5)
                val confirmItem = createConfirmItem(displayOnly)
                val invisibleConfirmItem = createConfirmItem(displayOnly, invisible = true)

                val confirmHandler: suspend () -> Unit = {
                    if (displayOnly) {
                        view.close()
                    } else {
                        handleConfirmation(player, name, origins, layer, consumeOrb, orbSlot, originCost, config)
                        view.close()
                    }
                }

                pane[5, 3] = StaticElement(drawable(confirmItem)) {
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f)
                    completingLater = true
                    CoroutineScope(bukkitDispatcher).launch {
                        confirmHandler()
                        complete()
                    }
                }

                pane[5, 4] = StaticElement(drawable(invisibleConfirmItem)) {
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f)
                    completingLater = true
                    CoroutineScope(bukkitDispatcher).launch {
                        confirmHandler()
                        complete()
                    }
                }

                pane[5, 5] = StaticElement(drawable(invisibleConfirmItem)) {
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f)
                    completingLater = true
                    CoroutineScope(bukkitDispatcher).launch {
                        confirmHandler()
                        complete()
                    }
                }
            }
        }

        ui.open(player)
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
        player: Player
    ): OriginData {
        val defaultCost = config.swapCommand.vault.defaultCost

        return if (page >= origins.size) {
            // Random option
            val excludedOriginNames = config.originSelection.randomOption.exclude

            val descriptionText = StringBuilder("You'll be assigned one of the following:\n\n")
            origins.forEach { origin ->
                if (origin.name.lowercase() !in excludedOriginNames.map { it.lowercase() }) {
                    descriptionText.append(origin.getNameForDisplay()).append("\n")
                }
            }

            OriginData(
                icon = OrbOfOrigin.orb.clone(),
                name = "Random",
                nameForDisplay = "Random",
                impact = '\uE002',
                data = LineData(LineData.makeLineFor(descriptionText.toString(), LineData.LineComponent.LineType.DESCRIPTION)),
                cost = defaultCost
            )
        } else {
            val origin = origins[page]
            OriginData(
                icon = origin.icon,
                name = origin.name,
                nameForDisplay = origin.getNameForDisplay(),
                impact = origin.impactChar,
                data = origin.getLineData(),
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
        // Compress name with spacer characters
        val compressedName = buildString {
            append("\uF001")
            nameForDisplay.forEach { c ->
                append(c)
                append('\uF000')
            }
        }

        val background = TextRenderingUtils.applyFont(
            ShortcutUtils.getColored(config.originSelection.screenTitle.background),
            Key.key("minecraft:default")
        )

        var component = TextRenderingUtils.applyFont(
            Component.text("\uF000\uE000\uF001\uE001\uF002$impact"),
            Key.key("minecraft:origin_selector")
        )
            .color(NamedTextColor.WHITE)
            .append(background)
            .append(
                TextRenderingUtils.applyFont(
                    Component.text(compressedName),
                    Key.key("minecraft:origin_title_text")
                ).color(NamedTextColor.WHITE)
            )
            .append(
                TextRenderingUtils.applyFont(
                    Component.text(TextRenderingUtils.getInverseForString(nameForDisplay) + "\uF000"),
                    Key.key("minecraft:reverse_text")
                ).color(NamedTextColor.WHITE)
            )

        // Add description lines
        data.getLines(scrollAmount).forEach { line ->
            component = component.append(line)
        }

        val prefix = TextRenderingUtils.applyFont(
            ShortcutUtils.getColored(config.originSelection.screenTitle.prefix),
            Key.key("minecraft:default")
        )
        val suffix = TextRenderingUtils.applyFont(
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
        item.itemMeta = NMSInvoker.setCustomModelData(meta, customModelData)
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
        item.itemMeta = NMSInvoker.setCustomModelData(meta, cmd)
        return item
    }

    private fun createConfirmItem(displayOnly: Boolean, invisible: Boolean = false): ItemStack {
        val item = ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
        var meta = item.itemMeta

        meta.displayName(
            Component.text(if (displayOnly) "Close" else "Confirm")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
        )

        meta = NMSInvoker.setCustomModelData(meta, if (invisible) 6 else 5)
        item.itemMeta = meta
        return item
    }

    private suspend fun handleConfirmation(
        player: Player,
        originName: String,
        origins: List<Origin>,
        layer: String,
        consumeOrb: Boolean,
        orbSlot: Int,
        costAmount: Int,
        config: MainConfig
    ) {
        val container = v2Container ?: return

        // Handle cost if vault is enabled and applicable
        if (ru.turbovadim.OriginsReforged.instance.isVaultEnabled && costAmount != 0 &&
            !player.hasPermission(config.swapCommand.vault.bypassPermission)) {
            val economy = ru.turbovadim.OriginsReforged.instance.economy
            if (economy == null || !economy.has(player, costAmount.toDouble())) {
                player.sendMessage(Component.text("You don't have enough money.", NamedTextColor.RED))
                return
            }
            economy.withdrawPlayer(player, costAmount.toDouble())
        }

        val origin = if (originName.equals("random", ignoreCase = true)) {
            val excludedOrigins = config.originSelection.randomOption.exclude.map { it.lowercase() }
            val availableOrigins = origins.filter { o ->
                o.name.lowercase() !in excludedOrigins
            }
            if (availableOrigins.isEmpty()) {
                origins.firstOrNull()
            } else {
                availableOrigins.random()
            }
        } else {
            container.originRegistry.getByName(originName)
        }

        if (origin == null) {
            player.sendMessage(Component.text("Origin not found.", NamedTextColor.RED))
            return
        }

        // Set the origin
        container.playerStateManager.setOrigin(player, layer, origin)

        player.sendMessage(
            Component.text("You are now a ")
                .color(NamedTextColor.GREEN)
                .append(origin.displayName.color(NamedTextColor.GOLD))
                .append(Component.text("!").color(NamedTextColor.GREEN))
        )

        // Consume orb if needed
        if (consumeOrb && orbSlot >= 0 && mainConfig.orbOfOrigin.consume) {
            val item = player.inventory.getItem(orbSlot)
            if (item != null && item.amount > 0) {
                item.amount -= 1
            }
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
}
