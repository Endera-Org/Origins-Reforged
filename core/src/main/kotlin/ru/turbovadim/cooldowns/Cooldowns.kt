package ru.turbovadim.cooldowns

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.endera.enderalib.utils.async.ioDispatcher
import org.intellij.lang.annotations.Subst
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsRebornEnhanced
import ru.turbovadim.OriginsRebornEnhanced.Companion.NMSInvoker
import ru.turbovadim.OriginsRebornEnhanced.Companion.instance
import ru.turbovadim.ShortcutUtils.isBedrockPlayer
import ru.turbovadim.events.PlayerSwapOriginEvent
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.time.Instant
import javax.imageio.ImageIO
import kotlin.math.floor
import kotlin.math.max

class Cooldowns : Listener {

    private val registeredCooldowns: MutableMap<NamespacedKey?, CooldownInfo> = HashMap<NamespacedKey?, CooldownInfo>()
    private val cooldownKey = NamespacedKey(instance, "cooldowns")
    private val hasCooldownKey = NamespacedKey(instance, "has_cooldown")

    @EventHandler
    fun onPlayerSwapOrigin(event: PlayerSwapOriginEvent) {
        event.getPlayer().persistentDataContainer.remove(cooldownKey)
    }

    // Создаём канал для сигналов тика
    private val tickChannel = Channel<Unit>(Channel.CONFLATED)

    init {
        CoroutineScope(ioDispatcher).launch {
            for (signal in tickChannel) {
                processTick()
            }
        }
    }

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        tickChannel.trySend(Unit)
    }

    private suspend fun processTick() {
        val now = Instant.now().toEpochMilli()
        val updates = mutableListOf<Pair<Player, Component>>()

        for (player in Bukkit.getOnlinePlayers()) {
            val playerPDC = player.persistentDataContainer
            val cooldownPDC = playerPDC.getOrDefault(
                cooldownKey,
                PersistentDataType.TAG_CONTAINER,
                playerPDC.adapterContext.newPersistentDataContainer()
            )

            val cooldownKeys = getActiveCooldownKeys(cooldownPDC, now)
            if (cooldownKeys.isEmpty()) {
                if (playerPDC.has(hasCooldownKey, OriginSwapper.BooleanPDT.BOOLEAN)) {
                    playerPDC.remove(hasCooldownKey)
                }
                continue
            }
            playerPDC.set(hasCooldownKey, OriginSwapper.BooleanPDT.BOOLEAN, true)

            val message: Component = if (isBedrockPlayer(player.uniqueId)) {
                val sb = StringBuilder()
                for (key in cooldownKeys) {
                    val info = registeredCooldowns[key]
                    if (info == null || info.icon == null) continue
                    val remaining = cooldownPDC.getOrDefault(
                        key,
                        PersistentDataType.LONG,
                        0L
                    ) - (if (info.isStatic) 0 else now)
                    val secondsRemaining = (remaining / 50).toInt()
                    val timeStr: String? = getTime(secondsRemaining)
                    if (timeStr != null) {
                        sb.append(timeStr).append(" ")
                    }
                }
                Component.text(sb.toString())
            } else {
                var heightOffset = computeHeightOffset(player)
                var msg: Component = Component.empty()
                for (key in cooldownKeys) {
                    val info = registeredCooldowns[key]
                    if (info == null || info.icon == null) continue
                    val remaining = cooldownPDC.getOrDefault<Long?, Long?>(
                        key,
                        PersistentDataType.LONG,
                        0L
                    ) - (if (info.isStatic) 0 else now)
                    var ratio = remaining / (info.cooldownTime * 50f)
                    if (!info.isReversed) {
                        ratio = 1 - ratio
                    }
                    msg = msg
                        .append(Component.text("\uF004"))
                        .append(formCooldownBar(ratio, info, heightOffset))
                    heightOffset++
                }
                NMSInvoker.applyFont(
                    Component.text("\uF003"),
                    Key.key("minecraft:cooldown_bar/height_0")
                ).append(msg)
            }
            updates.add(Pair(player, message))
        }

        withContext(OriginsRebornEnhanced.bukkitDispatcher) {
            for ((player, message) in updates) {
                player.sendActionBar(message)
            }
        }
    }



    /**
     * Возвращает список активных кулдаунов для игрока.
     * Фильтруются ключи, для которых не зарегистрирован CooldownInfo,
     * либо оставшееся время <= 0 (если кулдаун не статичный).
     */
    private fun getActiveCooldownKeys(cooldownPDC: PersistentDataContainer, now: Long): MutableList<NamespacedKey> {
        val keys: MutableList<NamespacedKey> = ArrayList<NamespacedKey>(cooldownPDC.keys)
        keys.removeIf { key: NamespacedKey? ->
            val info = registeredCooldowns[key]
            if (info == null) return@removeIf true
            val remaining = cooldownPDC.getOrDefault<Long?, Long?>(
                key!!,
                PersistentDataType.LONG,
                0L
            ) - (if (info.isStatic) 0 else now)
            remaining <= 0 && !info.isStatic
        }
        return keys
    }

    /**
     * Вычисляет смещение (offset) для отображения полос кулдауна.
     * Учитываются: наличие транспорта (и его здоровье) и состояние под водой.
     */
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

    /**
     * Перегруженный метод getCooldown, использующий предварительно вычисленное время.
     */
    fun getCooldown(player: Player, key: NamespacedKey, now: Long): Long {
        val pdc = player.persistentDataContainer.getOrDefault<PersistentDataContainer?, PersistentDataContainer>(
            cooldownKey,
            PersistentDataType.TAG_CONTAINER,
            player.persistentDataContainer.adapterContext.newPersistentDataContainer()
        )
        val info = registeredCooldowns[key]
        if (info == null) return 0
        return max(
            0.0,
            (pdc.getOrDefault<Long?, Long?>(
                key,
                PersistentDataType.LONG,
                0L
            ) - (if (info.isStatic) 0 else now)).toDouble()
        ).toLong()
    }


    suspend fun formCooldownBar(percentage: Float, info: CooldownInfo, height: Int): Component {
        return iconDataMap[info.icon]!!.assemble(percentage, height)
    }

    fun getCooldown(player: Player, key: NamespacedKey): Long {
        val pdc = player.persistentDataContainer.getOrDefault<PersistentDataContainer?, PersistentDataContainer>(
            cooldownKey,
            PersistentDataType.TAG_CONTAINER,
            player.persistentDataContainer.adapterContext.newPersistentDataContainer()
        )
        val info = registeredCooldowns[key]
        if (info == null) return 0
        return max(
            0.0,
            (pdc.getOrDefault<Long?, Long?>(key, PersistentDataType.LONG, 0L) - (if (info.isStatic) 0 else Instant.now()
                .toEpochMilli())).toDouble()
        ).toLong()
    }

    fun getCooldowns(player: Player): MutableList<NamespacedKey?> {
        val pdc = player.persistentDataContainer.getOrDefault<PersistentDataContainer?, PersistentDataContainer>(
            cooldownKey,
            PersistentDataType.TAG_CONTAINER,
            player.persistentDataContainer.adapterContext.newPersistentDataContainer()
        )
        val keys: MutableList<NamespacedKey?> = ArrayList<NamespacedKey?>(pdc.keys)
        keys.removeIf { key: NamespacedKey? ->
            !registeredCooldowns.containsKey(key) || (!hasCooldown(player, key!!) && !registeredCooldowns[key]!!.isStatic)
        }
        return keys
    }

    fun resetCooldowns(player: Player) {
        player.persistentDataContainer.remove(cooldownKey)
    }

    fun hasCooldown(player: Player, key: NamespacedKey): Boolean {
        return getCooldown(player, key) > 0
    }

    fun setCooldown(player: Player, key: NamespacedKey, cooldown: Int, isStatic: Boolean) {
        val pdc = player.persistentDataContainer.getOrDefault<PersistentDataContainer?, PersistentDataContainer>(
            cooldownKey,
            PersistentDataType.TAG_CONTAINER,
            player.persistentDataContainer.adapterContext.newPersistentDataContainer()
        )
        pdc.set<Long?, Long?>(
            key,
            PersistentDataType.LONG,
            if (isStatic) cooldown * 50L else Instant.now().toEpochMilli() + (cooldown * 50L)
        )
        player.persistentDataContainer
            .set<PersistentDataContainer?, PersistentDataContainer?>(cooldownKey, PersistentDataType.TAG_CONTAINER, pdc)
    }

    fun setCooldown(player: Player, key: NamespacedKey) {
        val info: CooldownInfo = registeredCooldowns[key]!!
        setCooldown(player, key, info.cooldownTime, info.isStatic)
    }

    @JvmRecord
    data class CooldownIconData(val barPieces: MutableList<Component?>?, val icon: Component?) {
        suspend fun assemble(completion: Float, height: Int): Component {
            val num = floor((barPieces!!.size * completion).toDouble())
            var result = icon!!.append(Component.text("\uF002"))
            for (i in barPieces.indices) {
                result = result.append((if (i <= num) barPieces else emptyBarPieces)[i]!!)
                result = result.append(Component.text("\uF001"))
            }
            @Subst("minecraft:cooldown_bar/height_0") val formatted = "minecraft:cooldown_bar/height_$height"
            return NMSInvoker.applyFont(result, Key.key(formatted))
        }
    }

    fun makeCID(file: File): CooldownIconData {
        val image: BufferedImage
        try {
            image = ImageIO.read(file)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return CooldownIconData(makeBarPieces(image), makeIcon(image))
    }

    fun makeIcon(image: BufferedImage): Component {
        val iconImage = image.getSubimage(73, 0, 8, 8)
        var icon: Component = Component.empty()
        val pixels = "\uE000\uE001\uE002\uE003\uE004\uE005\uE006\uE007"
        for (x in 0..7) {
            for (y in 0..7) {
                val col = iconImage.getRGB(x, y)
                icon = if (col == 0) {
                    icon.append(Component.text("\uF002"))
                } else icon.append(Component.text(pixels[y]).color(TextColor.color(col)))
                icon = icon.append(Component.text(if (y == 7) "\uF001" else "\uF000"))
            }
        }
        return icon
    }

    fun makeBarPieces(image: BufferedImage): MutableList<Component?> {
        val barImage = image.getSubimage(0, 2, 71, 5)
        val pixels = "\uE002\uE003\uE004\uE005\uE006"
        val result: MutableList<Component?> = ArrayList<Component?>()
        for (x in 0..70) {
            var c: Component = Component.empty()
            for (y in 0..4) {
                val col = barImage.getRGB(x, y)
                c = if (col == 0) {
                    c.append(Component.text("\uF002"))
                } else c.append(Component.text(pixels[y]).color(TextColor.color(col)))
                if (y != 4) c = c.append(Component.text("\uF000"))
            }
            result.add(c)
        }
        return result
    }

    val iconDataMap: MutableMap<String?, CooldownIconData?> = HashMap<String?, CooldownIconData?>()

    fun registerCooldown(instance: JavaPlugin, key: NamespacedKey, info: CooldownInfo): NamespacedKey {
        if (OriginsRebornEnhanced.mainConfig.cooldowns.disableAllCooldowns) return key

        if (info.icon != null && OriginsRebornEnhanced.mainConfig.cooldowns.showCooldownIcons) {
            val icon = File(instance.dataFolder, "icons/${info.icon}.png")
            if (!icon.exists()) {
                val ignored = icon.getParentFile().mkdirs()
                instance.saveResource("icons/${info.icon}.png", false)
            }
            val iconData = makeCID(icon)
            iconDataMap.put(info.icon, iconData)
        }
        if (!fileConfiguration.contains(key.toString())) {
            fileConfiguration.set(key.toString(), -1)
            try {
                fileConfiguration.save(file)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
        val i = fileConfiguration.getInt(key.toString())
        if (i != -1) info.cooldownTime = i
        registeredCooldowns.put(key, info)
        return key
    }

    private val file: File = File(instance.dataFolder, "cooldown-config.yml")

    private val fileConfiguration: FileConfiguration

    init {
        if (!file.exists()) {
            val ignored = file.getParentFile().mkdirs()
            instance.saveResource("cooldown-config.yml", false)
        }

        fileConfiguration = YamlConfiguration()

        try {
            fileConfiguration.load(file)
        } catch (e: InvalidConfigurationException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        val icon = File(instance.dataFolder, "icons/empty_bar.png")

        if (!icon.exists()) {
            val ignored = icon.getParentFile().mkdirs()
            instance.saveResource("icons/empty_bar.png", false)
        }

        val image: BufferedImage
        try {
            image = ImageIO.read(icon)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        emptyBarPieces = makeBarPieces(image)
    }

    @Suppress("unused")
    class CooldownInfo @JvmOverloads constructor(
        var cooldownTime: Int,
        val icon: String? = null,
        val isReversed: Boolean = false,
        val isStatic: Boolean = false
    ) {
        constructor(cooldownTime: Int, reversed: Boolean) : this(cooldownTime, null, reversed, false)
    }

    fun resetFile() {
        for (key in fileConfiguration.getKeys(false)) {
            fileConfiguration.set(key, null)
        }
        try {
            fileConfiguration.save(file)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        private fun getTime(cooldownTime: Int): String? {
            if (cooldownTime <= 0) return null
            val minutes = cooldownTime / 60
            val seconds = cooldownTime % 60
            return if (minutes == 0) {
                "${cooldownTime}s"
            } else {
                "${minutes}m ${seconds}s"
            }
        }

        lateinit var emptyBarPieces: MutableList<Component?>
    }
}
