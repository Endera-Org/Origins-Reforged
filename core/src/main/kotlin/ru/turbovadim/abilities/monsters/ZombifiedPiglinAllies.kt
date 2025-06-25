package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import ru.turbovadim.OriginSwapper
import ru.turbovadim.abilities.types.VisibleAbility

class ZombifiedPiglinAllies : VisibleAbility, Listener {
    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "Nearby Zombified Piglins will attack anything that that attacks you or that you attack.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "Terrifying Armies",
            OriginSwapper.LineData.LineComponent.LineType.TITLE
        )

    override fun getKey(): Key {
        return Key.key("monsterorigins:zombified_piglin_allies")
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val entity = event.entity
        if (entity is LivingEntity) {
            val (player, shouldRun) = when (val damager = event.damager) {
                is Player -> damager to true
                is Projectile -> {
                    val shooter = damager.shooter
                    if (shooter is Player) {
                        shooter to true
                    } else {
                        null to false
                    }
                }
                else -> null to false
            }

            if (shouldRun && player != null) {
                runForAbility(player) {
                    player.getNearbyEntities(32.0, 32.0, 32.0)
                        .filter { it.type == EntityType.ZOMBIFIED_PIGLIN }
                        .forEach { z ->
                            (z as PigZombie).apply {
                                isAngry = true
                                target = entity
                            }
                        }
                }
            }
        }

        val source = when (val dam = event.damager) {
            is LivingEntity -> dam
            is Projectile -> (dam.shooter as? LivingEntity)
            else -> null
        } ?: return

        runForAbility(event.entity) {
            event.entity.getNearbyEntities(32.0, 32.0, 32.0)
                .filter { it.type == EntityType.ZOMBIFIED_PIGLIN }
                .forEach { z ->
                    (z as PigZombie).apply {
                        isAngry = true
                        target = source
                    }
                }
        }
    }
}
