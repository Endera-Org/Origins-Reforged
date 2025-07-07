package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import ru.turbovadim.OriginSwapper
import ru.turbovadim.abilities.types.VisibleAbility

class WaterCombatant : VisibleAbility, Listener {

    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "You deal more damage while in water.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "Water Combatant",
            OriginSwapper.LineData.LineComponent.LineType.TITLE
        )

    override val key: Key
        get() = Key.key("moborigins:water_combatant")

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        runForAbility(event.damager, {
            if (event.damager.isInWater) {
                event.setDamage(event.damage + 3)
            }
        })
    }
}
