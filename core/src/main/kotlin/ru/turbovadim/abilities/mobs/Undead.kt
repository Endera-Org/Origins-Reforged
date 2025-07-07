package ru.turbovadim.abilities.mobs

import com.destroystokyo.paper.MaterialTags
import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.block.BlockFace
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility
import kotlin.math.max

class Undead : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "You are undead, and burn in the daylight. You also take more damage from smite.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Undead",
        LineComponent.LineType.TITLE
    )

    override val key: Key
        get() {
            return Key.key("moborigins:undead")
        }

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent?) {
        Bukkit.getOnlinePlayers().forEach { player ->
            runForAbility(player) { player ->
                val location = player.location
                var block = player.world.getHighestBlockAt(location)
                while ((MaterialTags.GLASS.isTagged(block) || MaterialTags.GLASS_PANES.isTagged(block)) && block.y >= location.y) {
                    block = block.getRelative(BlockFace.DOWN)
                }
                val isBelowPlayer = block.y < location.y

                val config = OriginsReforged.instance.config
                val overworld = config.getString("worlds.world") ?: run {
                    config.set("worlds.world", "world")
                    OriginsReforged.instance.saveConfig()
                    "world"
                }

                val isInOverworld = player.world === Bukkit.getWorld(overworld)
                val isDay = player.world.isDayTime

                if (isBelowPlayer && isInOverworld && isDay && !player.isInWaterOrRainOrBubbleColumn) {
                    player.fireTicks = max(player.fireTicks.toDouble(), 60.0).toInt()
                }
            }
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        runForAbility(event.entity) { player ->
            (event.damager as? LivingEntity)?.let { damager ->
                val level = damager.activeItem.getEnchantmentLevel(NMSInvoker.getSmiteEnchantment())
                event.damage += 2.5 * level
            }
        }
    }
}
