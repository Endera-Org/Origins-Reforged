package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility

class Bouncy : VisibleAbility, Listener {
    override val description: MutableList<LineComponent>
        get() = LineData.makeLineFor("All blocks act like slime blocks.", LineType.DESCRIPTION)

    override val title: MutableList<LineComponent>
        get() = LineData.makeLineFor("Bouncy", LineType.TITLE)

    override val key: Key = Key.key("moborigins:bouncy")

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        runForAbility(player) {
            if (player.isSneaking) return@runForAbility
            NMSInvoker.bounce(player)
        }
    }
}
