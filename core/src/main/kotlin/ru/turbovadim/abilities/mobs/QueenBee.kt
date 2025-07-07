package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.abilities.types.VisibleAbility

class QueenBee : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> =
        LineData.makeLineFor("When you collect honey, the bees won't try to attack you.", LineType.DESCRIPTION)

    override val title: MutableList<LineComponent> = LineData.makeLineFor("Queen Bee", LineType.TITLE)

    override val key: Key = Key.key("moborigins:queen_bee")

    @EventHandler
    fun onEntityTargetLivingEntity(event: EntityTargetLivingEntityEvent) {
        if (event.entity.type != EntityType.BEE) return
        if (event.reason != EntityTargetEvent.TargetReason.CLOSEST_PLAYER) return

        val target = event.target ?: return
        runForAbility(target) {
            event.isCancelled = true
        }
    }
}
