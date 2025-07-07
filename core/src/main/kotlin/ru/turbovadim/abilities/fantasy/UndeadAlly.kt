package ru.turbovadim.abilities.fantasy

import io.papermc.paper.tag.EntityTags
import net.kyori.adventure.key.Key
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.VisibleAbility

class UndeadAlly : VisibleAbility, Listener {
    override val key: Key = Key.key("fantasyorigins:undead_ally")

    override val description: MutableList<LineComponent> = makeLineFor(
        "As an undead monster, other undead creatures will not attack you unprovoked.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Undead Ally",
        LineComponent.LineType.TITLE
    )

    @EventHandler
    fun onEntityTargetLivingEntity(event: EntityTargetLivingEntityEvent) {
        if (!EntityTags.UNDEADS.isTagged(event.entityType)) return
        val target = event.target as? Player ?: return
        runForAbility(target) { _ ->
            val attacked = attackedEntities.getOrPut(target) { mutableListOf() }
            if (!attacked.contains(event.entity)) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val player = when (val damager = event.damager) {
            is Player -> damager
            is Projectile -> damager.shooter as? Player ?: return
            else -> return
        }
        attackedEntities.getOrPut(player) { mutableListOf() }.add(event.entity)
    }
    private val attackedEntities = mutableMapOf<Player, MutableList<Entity>>()
}
