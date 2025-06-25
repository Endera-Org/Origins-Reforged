package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityResurrectEvent
import org.bukkit.inventory.ItemStack
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.abilities.types.VisibleAbility
import java.util.*

class LowerTotemChance : VisibleAbility, Listener {
    override val description: MutableList<LineComponent>
        get() = LineData.makeLineFor("Totems have a 10% chance not to break on use.", LineType.DESCRIPTION)

    override val title: MutableList<LineComponent>
        get() = LineData.makeLineFor("Arcane Totems", LineType.TITLE)

    override fun getKey(): Key {
        return Key.key("moborigins:lower_totem_chance")
    }

    @EventHandler
    fun onEntityResurrect(event: EntityResurrectEvent) {
        val player = event.entity as? Player ?: return
        runForAbility(player) {
            val equipment = player.equipment
            if (random.nextDouble() < 0.9) return@runForAbility

            val newTotem = ItemStack(Material.TOTEM_OF_UNDYING)
            if (equipment.itemInMainHand.type == Material.TOTEM_OF_UNDYING) {
                equipment.setItemInMainHand(newTotem)
            } else {
                equipment.setItemInOffHand(newTotem)
            }
        }
    }

    private val random = Random()
}
