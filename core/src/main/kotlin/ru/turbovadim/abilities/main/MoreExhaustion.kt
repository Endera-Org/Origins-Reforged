package ru.turbovadim.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityExhaustionEvent
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.VisibleAbility

class MoreExhaustion : VisibleAbility, Listener {
    override val key: Key = Key.key("origins:more_exhaustion")

    override val description: MutableList<LineComponent> = makeLineFor(
        "You exhaust much quicker than others, thus requiring you to eat more.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Large Appetite",
        LineComponent.LineType.TITLE
    )

    @EventHandler
    fun onEntityExhaustion(event: EntityExhaustionEvent) {
        runForAbility(event.getEntity()) { player ->
            event.exhaustion = event.exhaustion * 1.6f
        }
    }
}
