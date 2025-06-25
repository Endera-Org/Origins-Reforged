package ru.turbovadim.abilities.mobs

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.VisibleAbility

class SnowTrail : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "You leave a trail of snow.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Snow Trail",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key = Key.key("moborigins:snow_trail")

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent?) {
        Bukkit.getOnlinePlayers().forEach { player ->
            this.runForAbility(player, ru.turbovadim.abilities.types.Ability.AbilityRunner { p ->
                val block = p.location.block
                if (block.type == Material.AIR) {
                    val snowData = Material.SNOW.createBlockData()
                    if (block.canPlace(snowData)) {
                        block.type = Material.SNOW
                    }
                }
            })
        }
    }
}
