package ru.turbovadim.abilities

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.World
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsRebornEnhanced

class NetherSpawn : DefaultSpawnAbility, VisibleAbility {
    override fun getKey(): Key {
        return Key.key("origins:nether_spawn")
    }

    override val world: World?
        get() {
            val nether = OriginsRebornEnhanced.mainConfig.worlds.worldNether
            return Bukkit.getWorld(nether) ?: Bukkit.getWorld("world_nether")
        }


    override val description: MutableList<LineComponent> = makeLineFor("Your natural spawn will be in the Nether.", LineComponent.LineType.DESCRIPTION)

    override val title: MutableList<LineComponent> = makeLineFor("Nether Inhabitant", LineComponent.LineType.TITLE)
}
