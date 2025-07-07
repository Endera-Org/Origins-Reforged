package ru.turbovadim.abilities.mobs

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Tag
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.events.PlayerSwapOriginEvent

class Overheat : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = LineData.makeLineFor(
        "You have a temperature bar that slowly begins to fill in hot biomes, and cool in other biomes.",
        LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = LineData.makeLineFor("Overheat", LineType.TITLE)

    override val key: Key = Key.key("moborigins:overheat")

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        if (event.tickNumber % 20 != 0) return
        Bukkit.getOnlinePlayers().forEach { player ->
            runForAbility(player) {
                val location = player.location
                val block = location.block
                val belowBlockType = block.getRelative(BlockFace.DOWN).type
                val currentTemp = Temperature.INSTANCE.getTemperature(player)
                val newTemp = if (block.temperature < 1 || Tag.ICE.isTagged(belowBlockType)) {
                    currentTemp - 1
                } else {
                    currentTemp + 1
                }
                Temperature.INSTANCE.setTemperature(player, newTemp)
            }
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        runForAbility(event.player) {
            Temperature.INSTANCE.setTemperature(event.player, 0)
        }
    }

    @EventHandler
    fun onPlayerSwapOrigin(event: PlayerSwapOriginEvent) {
        runForAbility(event.getPlayer()) {
            Temperature.INSTANCE.setTemperature(event.getPlayer(), 0)
        }
    }
}
