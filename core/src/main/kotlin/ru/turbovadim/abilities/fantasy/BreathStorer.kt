package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.Ability.AbilityRunner
import ru.turbovadim.abilities.types.VisibleAbility

class BreathStorer : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "By right clicking using an empty bottle, you can store your own Dragon's Breath.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Dragon's Breath",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key {
        return Key.key("fantasyorigins:breath_storer")
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return
        if (!event.action.isRightClick) return

        runForAbility(player) { _ ->
            if (item.type == Material.GLASS_BOTTLE) {
                item.amount--
                player.inventory.addItem(ItemStack(Material.DRAGON_BREATH))
                    .values.forEach { leftover ->
                        player.world.dropItemNaturally(player.location, leftover)
                    }
            }
        }
    }
}
