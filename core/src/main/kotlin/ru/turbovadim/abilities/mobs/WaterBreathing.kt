package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityAirChangeEvent
import ru.turbovadim.abilities.types.Ability

class WaterBreathing : Ability, Listener {

    override val key: Key
        get() = Key.key("moborigins:water_breathing")

    @EventHandler
    fun onEntityAirChange(event: EntityAirChangeEvent) {
        val player = event.getEntity() as? Player
        if (player != null) {
            runForAbility(player) { event.amount = player.maximumAir }
        }
    }
}
