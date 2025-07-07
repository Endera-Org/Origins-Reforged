package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityExhaustionEvent
import ru.turbovadim.OriginSwapper
import ru.turbovadim.abilities.types.VisibleAbility

class ZombieHunger : VisibleAbility, Listener {
    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "Your constant hunger for flesh makes you exhaust quicker than a human.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor("Zombie Hunger", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override val key: Key
        get() = Key.key("moborigins:zombie_hunger")

    @EventHandler
    fun onEntityExhaustion(event: EntityExhaustionEvent) {
        runForAbility(event.getEntity()) {
            event.exhaustion = event.exhaustion * 1.5f
        }
    }
}
