package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.VisibleAbility

class InfiniteArrows : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "Arrows you shoot are not used up.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Infinite Arrows",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key = Key.key("monsterorigins:infinite_arrows")

    @EventHandler
    fun onPlayerLaunchProjectile(event: EntityShootBowEvent) {
        val player = event.entity as? Player ?: return
        val arrow = event.consumable ?: return
        if (arrow.type != Material.ARROW) return
        runForAbility(player) {
            player.inventory.addItem(arrow)
        }
    }
}
