package ru.turbovadim.abilities.monsters

import io.papermc.paper.tag.EntityTags
import net.kyori.adventure.key.Key
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import ru.turbovadim.OriginSwapper
import ru.turbovadim.abilities.types.VisibleAbility

class UndeadAllyMonsters : VisibleAbility, Listener {

    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "Undead mobs don't attack you, unless you attack them first.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor("Undead Ally", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override fun getKey(): Key {
        return Key.key("monsterorigins:undead_ally")
    }

    @EventHandler
    fun onEntityTargetLivingEntity(event: EntityTargetLivingEntityEvent) {
        if (EntityTags.UNDEADS.isTagged(event.entityType)) {
            val player = event.target as? Player
            if (player != null) {
                runForAbility(player) {
                    if (!attackedEntities.getOrDefault(player, ArrayList<Entity?>()).contains(event.getEntity())) {
                        event.isCancelled = true
                    }
                }
            }
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val player = when (val damager = event.damager) {
            is Player -> damager
            is Projectile -> (damager.shooter as? Player) ?: return
            else -> return
        }
        val playerHitEntities = attackedEntities.getOrPut(player) { mutableListOf() }
        playerHitEntities += event.entity
    }


    private val attackedEntities: MutableMap<Player, MutableList<Entity>> = HashMap()
}
