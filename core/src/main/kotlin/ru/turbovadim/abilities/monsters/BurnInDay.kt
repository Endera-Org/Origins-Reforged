package ru.turbovadim.abilities.monsters

import com.destroystokyo.paper.MaterialTags
import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsReforged
import ru.turbovadim.abilities.types.VisibleAbility
import kotlin.math.max

class BurnInDay : VisibleAbility, Listener {

    @EventHandler
    fun onServerTickEnd(ignored: ServerTickEndEvent?) {
        for (player in Bukkit.getOnlinePlayers()) {
            runForAbility(player){
                var block = player.world.getHighestBlockAt(player.location)
                while ((MaterialTags.GLASS.isTagged(block) || (MaterialTags.GLASS_PANES.isTagged(block)) && block.y >= player.location.y)) {
                    block = block.getRelative(BlockFace.DOWN)
                }
                val height = block.y < player.location.y

                val overworld: String = OriginsReforged.mainConfig.worlds.world

                val isInOverworld = player.world === Bukkit.getWorld(overworld)
                val day = player.world.isDayTime
                if (height && isInOverworld && day && !player.isInWaterOrRainOrBubbleColumn) {
                    player.fireTicks = max(player.fireTicks, 60)
                }
            }
        }
    }

    override val description: MutableList<OriginSwapper.LineData.LineComponent> = OriginSwapper.LineData.makeLineFor(
        "You burn in daylight.",
        OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<OriginSwapper.LineData.LineComponent> =
        OriginSwapper.LineData.makeLineFor("Photoallergic", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override val key: Key = Key.key("monsterorigins:burn_in_day")
}
