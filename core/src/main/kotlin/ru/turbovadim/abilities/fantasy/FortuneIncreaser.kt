package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.inventory.ItemStack
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.Ability.AbilityRunner
import ru.turbovadim.abilities.types.VisibleAbility
import kotlin.math.max

class FortuneIncreaser : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "Your care and mastery in the art of extracting minerals results in a much higher yield from ores than other creatures.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Careful Miner",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key {
        return Key.key("fantasyorigins:fortune_increaser")
    }

    private val blocks = mutableMapOf<Player, MutableList<ItemStack>>()

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockDropItem(event: BlockDropItemEvent) {
        if (event.player.inventory.itemInMainHand.itemMeta == null) return

        runForAbility(event.player) { _ ->
            val storedItems = blocks[event.player]?.toMutableList() ?: mutableListOf()

            val droppedItems = event.items.map { it.itemStack }
            event.items.clear()

            droppedItems.forEach { dropped ->
                storedItems.forEachIndexed { index, stored ->
                    if (stored.amount > 0 && stored.type == dropped.type && stored.itemMeta == dropped.itemMeta) {
                        stored.amount = max(0, stored.amount - dropped.amount)
                        storedItems[index] = stored
                    }
                }
            }

            storedItems.addAll(droppedItems)
            storedItems.removeIf { it.amount <= 0 }

            val dropLocation = event.block.location.clone().add(0.5, 0.0, 0.5)
            storedItems.forEach { item ->
                event.items.add(event.block.world.dropItem(dropLocation, item))
            }
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val mainHandItem = event.player.inventory.itemInMainHand.clone()
        mainHandItem.itemMeta ?: return
        val fortune = NMSInvoker.getFortuneEnchantment()
        mainHandItem.addUnsafeEnchantment(fortune, mainHandItem.getEnchantmentLevel(fortune) + 2)
        blocks[event.player] = event.block.getDrops(mainHandItem, event.player).toMutableList()
    }
}
