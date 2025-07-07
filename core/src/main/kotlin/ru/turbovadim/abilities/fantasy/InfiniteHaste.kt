package ru.turbovadim.abilities.fantasy

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.potion.PotionEffect
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility

class InfiniteHaste : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "You're well trained in mining, so are much faster than a regular human.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Fast Miner",
        LineComponent.LineType.TITLE
    )

    override val key: Key = Key.key("fantasyorigins:infinite_haste")

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        if (event.tickNumber % 20 != 0) return

        Bukkit.getOnlinePlayers().forEach { player ->
            runForAbility(player) { p ->
                p.addPotionEffect(PotionEffect(NMSInvoker.hasteEffect, 30, 1))
            }
        }
    }
}
