package ru.turbovadim.abilities.main

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.Ability.AbilityRunner
import ru.turbovadim.abilities.types.DependantAbility
import ru.turbovadim.abilities.types.VisibleAbility

class HungerOverTime : DependantAbility, VisibleAbility, Listener {
    override val key: Key = Key.key("origins:hunger_over_time")

    override val description: MutableList<LineComponent> = makeLineFor(
        "Being phantomized causes you to become hungry.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Fast Metabolism",
        LineComponent.LineType.TITLE
    )

    override val dependencyKey: Key = Key.key("origins:phantomize")

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        if (event.tickNumber % 20 != 0) return

        Bukkit.getOnlinePlayers().forEach { player ->
            runForAbility(player, AbilityRunner { it.exhaustion += 0.812f })
        }
    }

}
