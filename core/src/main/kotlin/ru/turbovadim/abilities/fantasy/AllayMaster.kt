package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.entity.Allay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility

class AllayMaster : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "Your musical aura allows you to breed allays without playing music.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Allay Master",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key {
        return Key.key("fantasyorigins:allay_master")
    }

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        runForAbility(player) { _ ->
            val allay = event.rightClicked as? Allay ?: return@runForAbility
            val item = player.inventory.getItem(event.hand)
            if (item.type != Material.AMETHYST_SHARD) return@runForAbility
            if (!NMSInvoker.duplicateAllay(allay)) return@runForAbility

            event.isCancelled = true
            item.amount--
            when (event.hand) {
                EquipmentSlot.HAND -> player.swingMainHand()
                else -> player.swingOffHand()
            }
            player.inventory.setItem(event.hand, item)
        }
    }
}
