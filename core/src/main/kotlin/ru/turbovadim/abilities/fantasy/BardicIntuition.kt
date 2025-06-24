package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.Tag
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.Ability.AbilityRunner
import ru.turbovadim.abilities.types.VisibleAbility
import java.util.*

class BardicIntuition : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "Your musical energy will sometimes cause a creeper to drop a music disc, even without a skeleton.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Bardic Intuition",
        LineComponent.LineType.TITLE
    )

    private val random = Random()

    override fun getKey(): Key {
        return Key.key("fantasyorigins:bardic_intuition")
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        if (entity.type != EntityType.CREEPER) return
        val killer = entity.killer ?: return
        if (random.nextDouble() > 0.25) return

        runForAbility(killer) { _ ->
            val discs = Tag.ITEMS_CREEPER_DROP_MUSIC_DISCS.values.toList()
            val disc = discs[random.nextInt(discs.size)]

            event.drops.add(ItemStack(disc))
        }
    }
}
