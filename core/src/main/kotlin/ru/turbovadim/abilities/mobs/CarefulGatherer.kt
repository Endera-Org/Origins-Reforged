package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByBlockEvent
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.abilities.types.VisibleAbility

class CarefulGatherer : VisibleAbility, Listener {
    override val description: MutableList<LineComponent>
        get() = LineData.makeLineFor("Sweet Berry Bushes don't hurt you at all.", LineType.DESCRIPTION)

    override val title: MutableList<LineComponent>
        get() = LineData.makeLineFor("Careful Gatherer", LineType.TITLE)

    override val key: Key = Key.key("moborigins:careful_gatherer")

    @EventHandler
    fun onEntityDamage(event: EntityDamageByBlockEvent) {
        if (event.damager == null) return
        if (event.damager!!.type != Material.SWEET_BERRY_BUSH) return
        runForAbility(event.entity) { event.isCancelled = true }
    }
}
