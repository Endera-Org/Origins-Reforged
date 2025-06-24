package ru.turbovadim.abilities.fantasy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.events.PlayerSwapOriginEvent
import java.util.*

class VampiricTransformation : VisibleAbility, Listener {
    override fun getKey(): Key {
        return Key.key("fantasyorigins:vampiric_transformation")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        if (OriginsReforged.instance.config.getDouble("vampire-transform-chance", 1.0) >= 1)
            "You can transform other players into vampires by killing them."
        else
            "You sometimes transform other players into vampires by killing them.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Vampiric Transformation",
        LineComponent.LineType.TITLE
    )

    private val random = Random()

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        CoroutineScope(ioDispatcher).launch {
            val deceased = event.entity
            if (deceased !is Player) return@launch
            val killer = deceased.killer ?: return@launch
            runForAbilityAsync(killer) { _ ->
                val chance = OriginsReforged.instance.config.getDouble("vampire-transform-chance", 1.0)
                if (random.nextDouble() <= chance) {
                    OriginSwapper.setOrigin(
                        killer,
                        OriginSwapper.getOrigin(killer, "origin"),
                        PlayerSwapOriginEvent.SwapReason.DIED,
                        false,
                        "origin"
                    )
                    killer.sendMessage(
                        Component.text("You have transformed into a Vampire!").color(NamedTextColor.RED)
                    )
                }
            }
        }
    }
}
