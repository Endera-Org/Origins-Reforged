package ru.turbovadim.abilities

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.Ability.AbilityRunner

class AerialCombatant : VisibleAbility, Listener {
    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        runForAbility(event.damager, AbilityRunner { player: Player ->
            if (player.isGliding) event.setDamage(event.damage * 2)
        })
    }

    override fun getKey(): Key {
        return Key.key("origins:aerial_combatant")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
            "You deal substantially more damage while in Elytra flight.",
            LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<LineComponent> = makeLineFor("Aerial Combatant", LineComponent.LineType.TITLE)

}
