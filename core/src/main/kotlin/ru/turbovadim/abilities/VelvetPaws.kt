package ru.turbovadim.abilities

import net.kyori.adventure.key.Key
import org.bukkit.GameEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.GenericGameEvent
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.Ability.AbilityRunner

class VelvetPaws : VisibleAbility, Listener {
    override fun getKey(): Key {
        return Key.key("origins:velvet_paws")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "Your footsteps don't cause any vibrations which could otherwise be picked up by nearby lifeforms.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor("Velvet Paws", LineComponent.LineType.TITLE)

    @EventHandler
    fun onGenericGameEvent(event: GenericGameEvent) {
        if (event.event == GameEvent.STEP) {
            runForAbility(event.entity!!, AbilityRunner { player: Player? -> event.isCancelled = true })
        }
    }
}
