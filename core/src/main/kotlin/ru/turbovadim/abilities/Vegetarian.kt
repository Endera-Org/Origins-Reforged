package ru.turbovadim.abilities

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.VisibleAbility

class Vegetarian : VisibleAbility, Listener {

    var meat: MutableList<Material> = mutableListOf(
        Material.PORKCHOP,
        Material.COOKED_PORKCHOP,
        Material.BEEF,
        Material.COOKED_BEEF,
        Material.CHICKEN,
        Material.COOKED_CHICKEN,
        Material.RABBIT,
        Material.COOKED_RABBIT,
        Material.MUTTON,
        Material.COOKED_MUTTON,
        Material.RABBIT_STEW,
        Material.COD,
        Material.COOKED_COD,
        Material.TROPICAL_FISH,
        Material.SALMON,
        Material.COOKED_SALMON,
        Material.PUFFERFISH
    )

    @EventHandler
    fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        if (event.item.type == Material.POTION) return

        val player = event.player
        runForAbility(player) { p ->
            if (meat.contains(event.item.type)) {
                event.isCancelled = true
                event.item.amount -= 1
                p.addPotionEffect(PotionEffect(PotionEffectType.POISON, 300, 1, false, true))
            }
        }
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "You can't digest any meat.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Vegetarian",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key {
        return Key.key("origins:vegetarian")
    }
}
