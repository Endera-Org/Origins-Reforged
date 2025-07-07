package ru.turbovadim.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.Ability

class DamageFromSnowballs : Ability, Listener {
    override val key: Key = Key.key("origins:damage_from_snowballs")

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val projectile = event.entity
        if (projectile.type != EntityType.SNOWBALL) return

        val direction = projectile.location.direction
        val hitEntity = event.hitEntity
        if (hitEntity == null) return
        runForAbility(hitEntity) { player ->
            NMSInvoker.dealFreezeDamage(player, 3)
            NMSInvoker.knockback(player, 0.5, -direction.x, -direction.z)
        }
    }

}
