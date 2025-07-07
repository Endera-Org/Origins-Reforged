package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.VisibleAbility

class GuardianAllyMosters : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "Guardians don't attack you!",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Guardian Ally",
        LineComponent.LineType.TITLE
    )

    override val key: Key
        get() = Key.key("monsterorigins:guardian_ally")

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
