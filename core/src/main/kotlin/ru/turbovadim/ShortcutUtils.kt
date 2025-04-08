package ru.turbovadim

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.geysermc.api.Geyser
import org.geysermc.floodgate.api.FloodgateApi
import org.json.JSONException
import org.json.JSONObject
import ru.turbovadim.OriginsRebornEnhanced.Companion.NMSInvoker
import java.io.File
import java.io.IOException
import java.util.*

@Suppress("unused")
object ShortcutUtils {

    private val colorCodeRegex = "<#\\w{6}>".toRegex()

    @JvmStatic
    fun giveItemWithDrops(player: Player, vararg itemStacks: ItemStack?) {
        val validItems = itemStacks.filterNotNull().toTypedArray()
        player.inventory.addItem(*validItems).values.forEach { leftover ->
            player.world.dropItemNaturally(player.location, leftover)
        }
    }

    fun getLivingDamageSource(event: EntityDamageByEntityEvent): LivingEntity? {
        return when (val damageDealer = event.damager) {
            is Projectile -> damageDealer.shooter as? LivingEntity
            is LivingEntity -> damageDealer
            else -> null
        }
    }

    fun openJSONFile(file: File): JSONObject {
        return try {
            val data = file.readText()
            try {
                JSONObject(data)
            } catch (e: JSONException) {
                JSONObject()
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @JvmStatic
    fun isBedrockPlayer(uuid: UUID): Boolean {
        return try {
            FloodgateApi.getInstance().isFloodgatePlayer(uuid)
        } catch (e: NoClassDefFoundError) {
            try {
                Geyser.api().isBedrockPlayer(uuid)
            } catch (ex: NoClassDefFoundError) {
                false
            }
        }
    }

    fun getColored(text: String): Component {
        val colors = substringsBetween(text, "<", ">")
        val parts = text.split(colorCodeRegex).filter { it.isNotEmpty() }
        var component = Component.empty()
        parts.forEachIndexed { index, part ->
            val coloredText = if (index < colors.size) {
                Component.text(part).color(TextColor.fromHexString(colors[index]))
            } else {
                Component.text(part)
            }
            component = component.append(coloredText)
        }
        return component
    }

    fun substringsBetween(s: String, start: String, end: String): List<String> {
        val list = mutableListOf<String>()
        var currentIndex = 0
        while (true) {
            val startIdx = s.indexOf(start, currentIndex)
            if (startIdx == -1) break
            val endIdx = s.indexOf(end, startIdx + start.length)
            if (endIdx == -1) break
            list.add(s.substring(startIdx + start.length, endIdx))
            currentIndex = endIdx + end.length
        }
        return list
    }

    @JvmStatic
    fun isInfinite(effect: PotionEffect): Boolean {
        return if (NMSInvoker.supportsInfiniteDuration()) {
            effect.duration == -1
        } else effect.duration >= 20000
    }

    @JvmStatic
    fun infiniteDuration(): Int {
        return if (NMSInvoker.supportsInfiniteDuration()) -1 else 50000
    }
}