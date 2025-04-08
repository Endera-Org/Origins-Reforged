package ru.turbovadim.abilities

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.Ability.AbilityRunner

class Unwieldy : VisibleAbility, Listener {

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent?) {
        val shieldAbility = AbilityRunner { player ->
            player.setCooldown(Material.SHIELD, 1000)
        }
        Bukkit.getOnlinePlayers().forEach { player ->
            runForAbility(player, shieldAbility)
        }
    }


    override fun getKey(): Key {
        return Key.key("origins:no_shield")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "The way your hands are formed provide no way of holding a shield upright.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor("Unwieldy", LineComponent.LineType.TITLE)
}
