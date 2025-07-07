package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.abilities.types.VisibleAbility

class GuardianAlly : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = LineData.makeLineFor("Guardians don't attack you!", LineType.DESCRIPTION)

    override val title: MutableList<LineComponent> = LineData.makeLineFor("Guardian Ally", LineType.TITLE)

    override val key: Key = Key.key("moborigins:guardian_ally")

    @EventHandler
    fun onEntityTargetLivingEntity(event: EntityTargetLivingEntityEvent) {
        if (event.entityType == EntityType.GUARDIAN || event.entityType == EntityType.ELDER_GUARDIAN) {
            val target = event.target ?: return
            runForAbility(target) {
                event.isCancelled = true
            }
        }
    }
}
