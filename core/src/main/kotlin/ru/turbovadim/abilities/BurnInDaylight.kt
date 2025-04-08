package ru.turbovadim.abilities

import com.destroystokyo.paper.MaterialTags
import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.DependantAbility.DependencyType

open class BurnInDaylight : VisibleAbility, DependantAbility, Listener {

    override val dependencyType: DependencyType = DependencyType.INVERSE


    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        if (event.tickNumber % 15 != 0) return

        Bukkit.getOnlinePlayers().forEach { player ->
            runForAbility(player) { player ->
                val world = player.world
                val loc = player.location
                val playerY = loc.y
                var block = world.getHighestBlockAt(loc)

                while ((MaterialTags.GLASS.isTagged(block) || MaterialTags.GLASS_PANES.isTagged(block)) && block.y >= playerY) {
                    block = block.getRelative(BlockFace.DOWN)
                }

                val isBelowPlayer = block.y < playerY
                if (isBelowPlayer && world.environment == World.Environment.NORMAL && world.isDayTime && !player.isInWaterOrRainOrBubbleColumn) {
                    player.fireTicks = player.fireTicks.coerceAtLeast(60)
                }
            }
        }
    }


    override fun getKey(): Key {
        return Key.key("origins:burn_in_daylight")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
            "You begin to burn in daylight if you are not invisible.",
            LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<LineComponent> = makeLineFor("Photoallergic", LineComponent.LineType.TITLE)

    override val dependencyKey: Key = Key.key("origins:phantomize")
}
