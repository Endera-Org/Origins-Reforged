package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityAirChangeEvent
import ru.turbovadim.abilities.types.Ability

class WaterBreathingMonsters : Ability, Listener {
    @EventHandler
    fun onEntityAirChange(event: EntityAirChangeEvent) {
        val player = event.getEntity() as? Player
        if (player != null) {
            runForAbility(player) { event.amount = player.maximumAir }
        }
    }

    override val key: Key
        get() {
            return Key.key("monsterorigins:water_breathing")
        }
}
