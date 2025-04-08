package ru.turbovadim.abilities

import net.kyori.adventure.key.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.Ability.AbilityRunner
import ru.turbovadim.abilities.types.VisibleAbility

class FallImmunity : VisibleAbility, Listener {

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        if (event.cause != EntityDamageEvent.DamageCause.FALL) return

        runForAbility(event.entity, AbilityRunner { player ->
            event.isCancelled = true
        })
    }

    override fun getKey(): Key {
        return Key.key("origins:fall_immunity")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "You never take fall damage, no matter from which height you fall.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Acrobatics",
        LineComponent.LineType.TITLE
    )
}
