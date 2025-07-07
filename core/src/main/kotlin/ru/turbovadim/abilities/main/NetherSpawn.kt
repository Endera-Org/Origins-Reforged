package ru.turbovadim.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.World
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged
import ru.turbovadim.abilities.types.DefaultSpawnAbility
import ru.turbovadim.abilities.types.VisibleAbility

class NetherSpawn : DefaultSpawnAbility, VisibleAbility {
    override val key: Key = Key.key("origins:nether_spawn")

    override val world: World?
        get() {
            val nether = OriginsReforged.mainConfig.worlds.worldNether
            return Bukkit.getWorld(nether) ?: Bukkit.getWorld("world_nether")
        }


    override val description: MutableList<LineComponent> = makeLineFor("Your natural spawn will be in the Nether.", LineComponent.LineType.DESCRIPTION)

    override val title: MutableList<LineComponent> = makeLineFor("Nether Inhabitant", LineComponent.LineType.TITLE)
}
