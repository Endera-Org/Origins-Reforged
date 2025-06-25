package ru.turbovadim.abilities.mobs

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.VisibleAbility

class SurfaceWeakness : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "You are weakened while on land.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Surface Weakness",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key {
        return Key.key("moborigins:surface_weakness")
    }

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        Bukkit.getOnlinePlayers().forEach { player ->
            runForAbility(player) {
                if (!player.isInWater) {
                    player.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, -1, 0, true, true))
                } else {
                    player.getPotionEffect(PotionEffectType.WEAKNESS)
                        ?.takeIf { it.duration == -1 }
                        ?.let { player.removePotionEffect(PotionEffectType.WEAKNESS) }
                }
            }
        }
    }
}
