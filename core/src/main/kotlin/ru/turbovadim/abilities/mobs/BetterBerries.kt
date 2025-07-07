package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.abilities.types.VisibleAbility
import kotlin.math.min

class BetterBerries : VisibleAbility, Listener {
    override val description: MutableList<LineComponent>
        get() = LineData.makeLineFor("Berries taste extra delicious to you!", LineType.DESCRIPTION)

    override val title: MutableList<LineComponent>
        get() = LineData.makeLineFor("Better Berries", LineType.TITLE)

    override val key: Key = Key.key("moborigins:better_berries")

    @EventHandler
    fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        if (event.item.type != Material.SWEET_BERRIES) return
        val player = event.player
        runForAbility(player) {
            player.foodLevel = min((player.foodLevel + 2).toDouble(), 20.0).toInt()
            player.saturation = min((player.saturation + 1).toDouble(), player.foodLevel.toDouble()).toFloat()
        }
    }
}
