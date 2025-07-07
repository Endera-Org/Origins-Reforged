package ru.turbovadim.abilities.mobs

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.abilities.types.VisibleAbility

class ItemCollector : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> =
        LineData.makeLineFor("You have a larger item pickup radius.", LineType.DESCRIPTION)

    override val title: MutableList<LineComponent> = LineData.makeLineFor("Item Collector", LineType.TITLE)

    override val key: Key = Key.key("moborigins:item_collector")

    private fun pickUpItem(player: Player, item: Item) {
        val pickupEvent = EntityPickupItemEvent(player, item, 0)
        if (!pickupEvent.callEvent()) return

        val remainingItems = player.inventory.addItem(item.itemStack)
        if (remainingItems.isEmpty()) {
            player.playPickupItemAnimation(item)
            item.remove()
        } else {
            remainingItems.values.forEach { remainingStack ->
                val pickedUpAmount = item.itemStack.amount - remainingStack.amount
                player.playPickupItemAnimation(item, pickedUpAmount)
                item.itemStack = remainingStack
            }
        }
    }

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent?) {
        Bukkit.getOnlinePlayers().forEach { player ->
            runForAbility(player) {
                player.getNearbyEntities(2.5, 2.5, 2.5)
                    .filterIsInstance<Item>()
                    .filter { it.canPlayerPickup() && it.pickupDelay <= 0 }
                    .forEach { pickUpItem(player, it) }
            }
        }
    }
}
