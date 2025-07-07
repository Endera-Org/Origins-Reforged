package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.Location
import org.bukkit.entity.Piglin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PiglinBarterEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.loot.LootContext
import org.bukkit.loot.LootTables
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility
import java.util.*

class SuperBartering : VisibleAbility, Listener {
    override val description: MutableList<OriginSwapper.LineData.LineComponent> = OriginSwapper.LineData.makeLineFor(
        "You're brilliant at bartering after a lifetime of experience, every time you barter you get between 2 and 5 times as many valuables.",
        OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<OriginSwapper.LineData.LineComponent> = OriginSwapper.LineData.makeLineFor(
        "Bartering Master",
        OriginSwapper.LineData.LineComponent.LineType.TITLE
    )

    override val key: Key = Key.key("monsterorigins:super_bartering")

    @EventHandler
    fun onPiglinBarter(event: PiglinBarterEvent) {
        val player: Player? = NMSInvoker.getNearestVisiblePlayer(event.getEntity())
        if (player == null) return
        runForAbility(player) {
            val num: Int = random.nextInt(1, 4)
            for (i in 0..<num) {
                val items: MutableCollection<ItemStack> = getBarterResponseItems(event.getEntity())
                throwItemsTowardPlayer(event.getEntity(), player, items)
            }
        }
    }

    companion object {
        private val random = Random()

        private fun throwItemsTowardPos(piglin: Piglin, items: MutableCollection<ItemStack>, pos: Location) {
            for (itemStack in items) {
                NMSInvoker.throwItem(piglin, itemStack, pos.add(0.0, 1.0, 0.0))
            }
        }

        private fun throwItemsTowardPlayer(piglin: Piglin, player: Player, items: MutableCollection<ItemStack>) {
            throwItemsTowardPos(piglin, items, player.location)
        }

        private fun getBarterResponseItems(piglin: Piglin): MutableCollection<ItemStack> {
            return LootTables.PIGLIN_BARTERING.lootTable
                .populateLoot(random, LootContext.Builder(piglin.location).lootedEntity(piglin).build())
        }
    }
}
