package ru.turbovadim.v2.cooldown

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerActionBar
import kotlinx.coroutines.*
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.endera.enderalib.utils.async.ioDispatcher
import org.endera.enderalib.utils.async.EntityScheduler
import org.intellij.lang.annotations.Subst
import ru.turbovadim.OriginsReforged
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.ShortcutUtils.isBedrockPlayer
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.math.floor

/**
 * Manages ability cooldowns with visual progress bar display.
 * Uses textured icons and bars from PNG images with custom fonts.
 */
class CooldownManager {

    private val playerCooldowns = ConcurrentHashMap<UUID, MutableMap<Key, CooldownData>>()
    private val iconDataMap = ConcurrentHashMap<String, CooldownIconData>()
    private var tickJob: Job? = null
    private val lastDisplays = ConcurrentHashMap<UUID, Component>()
    private val missingIcons = ConcurrentHashMap.newKeySet<String>()
    private lateinit var emptyBarPieces: List<Component>

    data class CooldownData(
        val startTime: Long,
        val durationTicks: Int,
        val icon: String? = null
    ) {
        fun getRemainingTicks(): Int {
            val elapsed = (System.currentTimeMillis() - startTime) / 50
            return (durationTicks - elapsed).coerceAtLeast(0).toInt()
        }

        fun getProgress(): Float {
            val remaining = getRemainingTicks()
            return (remaining.toFloat() / durationTicks).coerceIn(0f, 1f)
        }

        fun isExpired(): Boolean = getRemainingTicks() <= 0
    }

    data class CooldownIconData(
        val barPieces: List<Component>,
        val icon: Component
    ) {
        private val renderedBars = ConcurrentHashMap<Pair<Int, Int>, Component>()

        fun assemble(completion: Float, height: Int): Component {
            val filledCount = floor(barPieces.size * completion).toInt().coerceIn(0, barPieces.size)
            return renderedBars.computeIfAbsent(filledCount to height) { assemble(filledCount, height) }
        }

        private fun assemble(filledCount: Int, height: Int): Component {
            var result = icon.append(Component.text("\uF002"))

            for (i in barPieces.indices) {
                val piece = if (i < filledCount) barPieces[i] else emptyBarPieces[i]
                result = result.append(piece)
                result = result.append(Component.text("\uF001"))
            }

            @Subst("minecraft:cooldown_bar/height_0")
            val formatted = "minecraft:cooldown_bar/height_$height"
            return NMSInvoker.applyFont(result, Key.key(formatted))
        }

        companion object {
            lateinit var emptyBarPieces: List<Component>
        }
    }

    fun start() {
        loadEmptyBar()

        tickJob = CoroutineScope(ioDispatcher).launch {
            while (isActive) {
                updateCooldownDisplays()
                delay(50)
            }
        }
    }

    fun stop() {
        tickJob?.cancel()
        tickJob = null
        playerCooldowns.clear()
        lastDisplays.clear()
    }

    private fun loadEmptyBar() {
        val plugin = OriginsReforged.instance
        val iconFile = File(plugin.dataFolder, "icons/empty_bar.png")

        if (!iconFile.exists()) {
            iconFile.parentFile.mkdirs()
            if (plugin.getResource("icons/empty_bar.png")?.use { true } == true) plugin.saveResource("icons/empty_bar.png", false)
        }

        if (iconFile.exists()) {
            val image = ImageIO.read(iconFile)
            emptyBarPieces = makeBarPieces(image)
            CooldownIconData.emptyBarPieces = emptyBarPieces
        } else {
            // Fallback to simple bar if no image
            emptyBarPieces = List(71) { Component.text("░") }
            CooldownIconData.emptyBarPieces = emptyBarPieces
        }
    }

    fun registerIcon(icon: String) {
        if (iconDataMap.containsKey(icon) || icon in missingIcons) return

        val plugin = OriginsReforged.instance
        val iconFile = File(plugin.dataFolder, "icons/$icon.png")

        if (!iconFile.exists()) {
            iconFile.parentFile.mkdirs()
            if (plugin.getResource("icons/$icon.png")?.use { true } != true) {
                missingIcons.add(icon)
                return
            }
            plugin.saveResource("icons/$icon.png", false)
        }

        if (iconFile.exists()) {
            val image = ImageIO.read(iconFile)
            iconDataMap[icon] = CooldownIconData(
                barPieces = makeBarPieces(image),
                icon = makeIcon(image)
            )
        }
    }

    fun setCooldown(player: Player, abilityKey: Key, durationTicks: Int, icon: String? = null) {
        if (OriginsReforged.mainConfig.cooldowns.disableAllCooldowns || durationTicks <= 0) return

        icon?.let { registerIcon(it) }

        val cooldowns = playerCooldowns.getOrPut(player.uniqueId) { ConcurrentHashMap() }
        cooldowns[abilityKey] = CooldownData(
            startTime = System.currentTimeMillis(),
            durationTicks = durationTicks,
            icon = icon
        )
    }

    fun removePlayer(playerId: UUID) {
        playerCooldowns.remove(playerId)
        lastDisplays.remove(playerId)
    }

    fun hasCooldown(player: Player, abilityKey: Key): Boolean {
        if (OriginsReforged.mainConfig.cooldowns.disableAllCooldowns) return false

        val cooldowns = playerCooldowns[player.uniqueId] ?: return false
        val data = cooldowns[abilityKey] ?: return false
        if (data.isExpired()) {
            cooldowns.remove(abilityKey)
            return false
        }
        return true
    }

    fun getCooldown(player: Player, abilityKey: Key): Int {
        val cooldowns = playerCooldowns[player.uniqueId] ?: return 0
        val data = cooldowns[abilityKey] ?: return 0
        return data.getRemainingTicks()
    }

    fun clearCooldowns(player: Player) {
        playerCooldowns[player.uniqueId]?.clear()
    }

    private fun updateCooldownDisplays() {
        if (!OriginsReforged.mainConfig.cooldowns.showCooldownIcons) return

        for ((playerId, cooldowns) in playerCooldowns) {
            val player = Bukkit.getPlayer(playerId) ?: continue
            EntityScheduler.execute(OriginsReforged.instance, player, {
                if (!player.isOnline || playerCooldowns[playerId] !== cooldowns) return@execute
                cooldowns.entries.removeIf { it.value.isExpired() }

                val message = if (cooldowns.isEmpty()) {
                    if (lastDisplays.remove(playerId) == null) return@execute
                    Component.empty()
                } else {
                    val rendered = if (isBedrockPlayer(playerId)) buildBedrockMessage(cooldowns.values)
                        else buildJavaMessage(player, cooldowns.values)
                    if (lastDisplays.put(playerId, rendered) == rendered) return@execute
                    rendered
                }
                PacketEvents.getAPI().playerManager.sendPacket(player, WrapperPlayServerActionBar(message))
            })
        }
    }

    private fun buildBedrockMessage(cooldowns: Collection<CooldownData>): Component {
        val sb = StringBuilder()
        for (cooldown in cooldowns) {
            val secondsRemaining = cooldown.getRemainingTicks() / 20
            val timeStr = getTimeString(secondsRemaining)
            if (timeStr != null) {
                sb.append(timeStr).append(" ")
            }
        }
        return Component.text(sb.toString())
    }

    private fun buildJavaMessage(player: Player, cooldowns: Collection<CooldownData>): Component {
        var heightOffset = computeHeightOffset(player)
        var msg = Component.empty()

        for (cooldown in cooldowns) {
            val icon = cooldown.icon
            val iconData = if (icon != null) iconDataMap[icon] else null

            if (iconData != null) {
                val ratio = cooldown.getProgress()
                msg = msg
                    .append(Component.text("\uF004"))
                    .append(iconData.assemble(1f - ratio, heightOffset))
                heightOffset++
            } else {
                msg = msg.append(Component.text("${cooldown.getRemainingTicks() / 20 + 1}s "))
            }
        }

        return NMSInvoker.applyFont(
            Component.text("\uF003"),
            Key.key("minecraft:cooldown_bar/height_0")
        ).append(msg)
    }

    private fun computeHeightOffset(player: Player): Int {
        var offset = 0
        val vehicle = player.vehicle
        if (vehicle is LivingEntity) {
            val instance = vehicle.getAttribute(NMSInvoker.maxHealthAttribute)
            if (instance != null) {
                offset += (floor((instance.value - 1) / 10) - 1).toInt()
            }
        }
        if (player.remainingAir < player.maximumAir || NMSInvoker.isUnderWater(player)) {
            offset++
        }
        return offset
    }

    private fun makeBarPieces(image: BufferedImage): List<Component> {
        val barImage = image.getSubimage(0, 2, 71, 5)
        val pixels = "\uE002\uE003\uE004\uE005\uE006"
        val result = mutableListOf<Component>()

        for (x in 0 until 71) {
            var c = Component.empty()
            for (y in 0 until 5) {
                val col = barImage.getRGB(x, y)
                c = if (col == 0) {
                    c.append(Component.text("\uF002"))
                } else {
                    c.append(Component.text(pixels[y]).color(TextColor.color(col)))
                }
                if (y != 4) c = c.append(Component.text("\uF000"))
            }
            result.add(c)
        }
        return result
    }

    private fun makeIcon(image: BufferedImage): Component {
        val iconImage = image.getSubimage(73, 0, 8, 8)
        var icon = Component.empty()
        val pixels = "\uE000\uE001\uE002\uE003\uE004\uE005\uE006\uE007"

        for (x in 0 until 8) {
            for (y in 0 until 8) {
                val col = iconImage.getRGB(x, y)
                icon = if (col == 0) {
                    icon.append(Component.text("\uF002"))
                } else {
                    icon.append(Component.text(pixels[y]).color(TextColor.color(col)))
                }
                icon = icon.append(Component.text(if (y == 7) "\uF001" else "\uF000"))
            }
        }
        return icon
    }

    private fun getTimeString(cooldownTime: Int): String? {
        if (cooldownTime <= 0) return null
        val minutes = cooldownTime / 60
        val seconds = cooldownTime % 60
        return if (minutes == 0) {
            "${cooldownTime}s"
        } else {
            "${minutes}m ${seconds}s"
        }
    }
}
