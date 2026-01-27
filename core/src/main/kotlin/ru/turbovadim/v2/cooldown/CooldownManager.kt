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
        fun assemble(completion: Float, height: Int): Component {
            val filledCount = floor(barPieces.size * completion).toInt()
            var result = icon.append(Component.text("\uF002"))

            for (i in barPieces.indices) {
                val piece = if (i <= filledCount) barPieces[i] else emptyBarPieces[i]
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
    }

    private fun loadEmptyBar() {
        val plugin = OriginsReforged.instance
        val iconFile = File(plugin.dataFolder, "icons/empty_bar.png")

        if (!iconFile.exists()) {
            iconFile.parentFile.mkdirs()
            plugin.saveResource("icons/empty_bar.png", false)
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
        if (iconDataMap.containsKey(icon)) return

        val plugin = OriginsReforged.instance
        val iconFile = File(plugin.dataFolder, "icons/$icon.png")

        if (!iconFile.exists()) {
            iconFile.parentFile.mkdirs()
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
        if (OriginsReforged.mainConfig.cooldowns.disableAllCooldowns) return

        icon?.let { registerIcon(it) }

        val cooldowns = playerCooldowns.getOrPut(player.uniqueId) { ConcurrentHashMap() }
        cooldowns[abilityKey] = CooldownData(
            startTime = System.currentTimeMillis(),
            durationTicks = durationTicks,
            icon = icon
        )
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
        playerCooldowns.remove(player.uniqueId)
    }

    private suspend fun updateCooldownDisplays() {
        if (!OriginsReforged.mainConfig.cooldowns.showCooldownIcons) return

        val updates = mutableListOf<Pair<Player, Component>>()

        for (player in Bukkit.getOnlinePlayers().toList()) {
            val cooldowns = playerCooldowns[player.uniqueId] ?: continue

            // Remove expired cooldowns
            cooldowns.entries.removeIf { it.value.isExpired() }

            if (cooldowns.isEmpty()) continue

            val message = if (isBedrockPlayer(player.uniqueId)) {
                buildBedrockMessage(cooldowns.values)
            } else {
                buildJavaMessage(player, cooldowns.values)
            }

            updates.add(player to message)
        }

        for ((player, message) in updates) {
            PacketEvents.getAPI().playerManager.sendPacket(
                player,
                WrapperPlayServerActionBar(message)
            )
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
