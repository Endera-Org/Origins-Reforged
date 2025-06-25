package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.Ability.AbilityRunner
import ru.turbovadim.abilities.types.VisibleAbility

class DoubleFireDamage : VisibleAbility, Listener {

    @EventHandler
    fun onEntityDamageEvent(event: EntityDamageEvent) {
        val fireCauses = setOf(
            EntityDamageEvent.DamageCause.FIRE,
            EntityDamageEvent.DamageCause.FIRE_TICK,
            EntityDamageEvent.DamageCause.LAVA,
            EntityDamageEvent.DamageCause.HOT_FLOOR
        )

        if (event.cause in fireCauses) {
            runForAbility(event.entity, AbilityRunner { _ ->
                event.damage = event.damage * 2
            })
        }
    }

    override fun getKey(): Key {
        return Key.key("monsterorigins:double_fire_damage")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "You take double damage from all sources of fire.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Frozen Skin",
        LineComponent.LineType.TITLE
    )
}
