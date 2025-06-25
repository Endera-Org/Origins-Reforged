package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.FoodLevelChangeEvent
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.VisibleAbility
import kotlin.math.min

class HalfMaxSaturation : VisibleAbility, Listener {
    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        val player = event.entity as? Player ?: return
        runForAbility(player) {
            player.saturation = min(player.saturation, player.foodLevel.toFloat() / 2)
        }
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "You can only hold half as much saturation as a human.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Poor Digestion",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key = Key.key("monsterorigins:half_max_saturation")
}
