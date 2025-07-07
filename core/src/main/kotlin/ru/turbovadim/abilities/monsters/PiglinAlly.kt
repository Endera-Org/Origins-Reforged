package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.VisibleAbility

class PiglinAlly : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "Piglins don't attack you, unless you attack them first.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Piglin Ally",
        LineComponent.LineType.TITLE
    )

    override val key: Key
        get() = Key.key("monsterorigins:piglin_ally")

    private val attackedEntities: MutableMap<Player, MutableList<Entity>> = mutableMapOf()

    @EventHandler
    fun onEntityTargetLivingEntity(event: EntityTargetLivingEntityEvent) {
        val target = event.target as? Player ?: return
        if (event.entityType == EntityType.PIGLIN || event.entityType == EntityType.PIGLIN_BRUTE) {
            runForAbility(target) {
                val alreadyAttacked = attackedEntities[target]?.contains(event.entity) ?: false
                if (!alreadyAttacked) {
                    event.isCancelled = true
                }
            }
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val player: Player? = when (val damager = event.damager) {
            is Player -> damager
            is Projectile -> damager.shooter as? Player
            else -> null
        }
        if (player != null) {
            val entityList = attackedEntities.getOrPut(player) { mutableListOf() }
            entityList.add(event.entity)
        }
    }
}
