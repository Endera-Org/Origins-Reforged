package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.Ability.AbilityRunner
import ru.turbovadim.abilities.types.VisibleAbility

class IncreasedArrowSpeed : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "All arrows you shoot move much faster through the air.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Swift Shot",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key {
        return Key.key("fantasyorigins:increased_arrow_speed")
    }

    @EventHandler
    fun onEntityShootBow(event: EntityShootBowEvent) {
        runForAbility(event.entity) { _ ->
            event.projectile.velocity = event.projectile.velocity.multiply(2.0)
        }
    }
}
