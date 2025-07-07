package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Arrow
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility

class BowBurst : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "By casting a spell on any regular arrow, you can instantly shoot 3 arrows at once using only one, but this disables your bow for 7 seconds.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Bow Burst",
        LineComponent.LineType.TITLE
    )

    override val key: Key = Key.key("fantasyorigins:bow_burst")

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (!event.action.isLeftClick) return
        val item = event.item ?: return
        if (item.type != Material.BOW) return

        val player = event.player
        if (player.getCooldown(Material.BOW) > 0) return

        runForAbility(player) { _ ->
            if (player.inventory.contains(Material.ARROW)) {
                player.inventory.firstOrNull { it?.type == Material.ARROW }?.let {
                    it.amount--
                }

                player.setCooldown(Material.BOW, 140)

                val arrow1 = player.launchProjectile<Arrow>(Arrow::class.java)
                val arrow2 = player.launchProjectile<Arrow>(Arrow::class.java)
                val arrow3 = player.launchProjectile<Arrow>(Arrow::class.java)

                NMSInvoker.launchArrow(arrow1, player, 0.0f, 3f, 15f)
                NMSInvoker.launchArrow(arrow2, player, 0.0f, 3f, 0f)
                NMSInvoker.launchArrow(arrow3, player, 0.0f, 3f, 15f)

                arrow1.pickupStatus = AbstractArrow.PickupStatus.CREATIVE_ONLY
                arrow2.pickupStatus = AbstractArrow.PickupStatus.CREATIVE_ONLY
                arrow3.pickupStatus = AbstractArrow.PickupStatus.CREATIVE_ONLY
            }
        }
    }
}
