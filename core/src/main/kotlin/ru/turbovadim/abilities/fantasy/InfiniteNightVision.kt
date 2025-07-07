package ru.turbovadim.abilities.fantasy

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

class InfiniteNightVision : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "Your eyes are adapted to see clearly in the dark.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Night Vision",
        LineComponent.LineType.TITLE
    )

    override val key: Key = Key.key("fantasyorigins:infinite_night_vision")

    private val nightVisionEffect = PotionEffect(PotionEffectType.NIGHT_VISION, 240, 0)

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        if (event.tickNumber % 15 != 0) return

        Bukkit.getOnlinePlayers().forEach { player ->
            runForAbility(player) {
                player.addPotionEffect(nightVisionEffect)
            }
        }
    }
}
