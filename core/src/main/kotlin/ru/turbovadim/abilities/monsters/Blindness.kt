package ru.turbovadim.abilities.monsters

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginSwapper
import ru.turbovadim.abilities.types.VisibleAbility

class Blindness : VisibleAbility, Listener {
    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "You can't see anything further than a few blocks away, though you can see further with night vision.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor("Blindness", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override val key: Key
        get() = Key.key("monsterorigins:blindness")

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        if (event.tickNumber % 5 != 0) return
        for (player in Bukkit.getOnlinePlayers()) {
            runForAbility(player) {
                if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                    player.removePotionEffect(PotionEffectType.BLINDNESS)
                    player.addPotionEffect(PotionEffect(PotionEffectType.DARKNESS, 240, 0, false, false))
                } else {
                    player.removePotionEffect(PotionEffectType.DARKNESS)
                    player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 240, 0, false, false))
                }
            }
        }
    }
}
