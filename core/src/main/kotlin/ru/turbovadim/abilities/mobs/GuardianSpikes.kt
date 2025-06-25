package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility
import java.util.*

class GuardianSpikes : VisibleAbility, Listener {
    override val description: MutableList<LineComponent>
        get() = LineData.makeLineFor("Spikes that have a chance to damage attackers!", LineType.DESCRIPTION)

    override val title: MutableList<LineComponent>
        get() = LineData.makeLineFor("Guardian Spikes", LineType.TITLE)

    override fun getKey(): Key {
        return Key.key("moborigins:guardian_spikes")
    }

    private val random = Random()

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        runForAbility(event.entity) {
            if (random.nextDouble() > 0.75) return@runForAbility
            NMSInvoker.dealThornsDamage(event.damager, 2, event.entity)
        }
    }
}
