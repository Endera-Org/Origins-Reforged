package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.VisibleAbility

class PerfectShot : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "Your arrows always fly perfectly straight and true.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Perfect Shot",
        LineComponent.LineType.TITLE
    )

    override val key: Key = Key.key("fantasyorigins:perfect_shot")

    @EventHandler
    fun onEntityShootBow(event: EntityShootBowEvent) {
        runForAbility(event.entity) { _ ->
            event.projectile.velocity =
                event.projectile.velocity.normalize().multiply(event.projectile.velocity.length())
        }
    }
}
