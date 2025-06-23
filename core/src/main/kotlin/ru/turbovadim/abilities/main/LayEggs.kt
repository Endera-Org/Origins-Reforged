package ru.turbovadim.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.TimeSkipEvent
import org.bukkit.inventory.ItemStack
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.Ability.AbilityRunner
import ru.turbovadim.abilities.types.VisibleAbility

class LayEggs : VisibleAbility, Listener {

    @EventHandler
    fun onTimeSkip(event: TimeSkipEvent) {
        if (event.skipReason != TimeSkipEvent.SkipReason.NIGHT_SKIP) return

        Bukkit.getOnlinePlayers()
            .filter { it.isDeeplySleeping }
            .forEach { player ->
                runForAbility(player, AbilityRunner {
                    player.world.dropItem(player.location, ItemStack(Material.EGG))
                    player.world.playSound(player.location, Sound.ENTITY_CHICKEN_EGG, SoundCategory.PLAYERS, 1f, 1f)
                })
            }
    }

    override fun getKey(): Key {
        return Key.key("origins:lay_eggs")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "Whenever you wake up in the morning, you will lay an egg.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Oviparous",
        LineComponent.LineType.TITLE
    )
}
