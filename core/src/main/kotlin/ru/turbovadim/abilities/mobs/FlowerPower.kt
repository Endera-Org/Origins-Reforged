package ru.turbovadim.abilities.mobs

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Tag
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.abilities.types.VisibleAbility

class FlowerPower : VisibleAbility, Listener {
    override val description: MutableList<LineComponent>
        get() = LineData.makeLineFor("When near multiple flowers, you gain regeneration.", LineType.DESCRIPTION)

    override val title: MutableList<LineComponent>
        get() = LineData.makeLineFor("Flower Power", LineType.TITLE)

    override val key: Key = Key.key("moborigins:flower_power")

    private val sphereOffsets: List<Vector> = (-3..3).flatMap { x ->
        (-3..3).flatMap { y ->
            (-3..3).mapNotNull { z ->
                val offset = Vector(x.toDouble(), y.toDouble(), z.toDouble())
                if (offset.length() <= 3.0) offset else null
            }
        }
    }

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        if (event.tickNumber % 40 != 0) return
        Bukkit.getOnlinePlayers().forEach { player ->
            runForAbility(player) {
                val baseLoc = player.location
                val flowerCount = sphereOffsets.count { offset ->
                    val loc = baseLoc.clone().add(offset)
                    Tag.FLOWERS.isTagged(loc.block.type)
                }
                if (flowerCount >= 3) {
                    player.addPotionEffect(
                        PotionEffect(PotionEffectType.REGENERATION, 200, 0, false, true)
                    )
                }
            }
        }
    }
}
