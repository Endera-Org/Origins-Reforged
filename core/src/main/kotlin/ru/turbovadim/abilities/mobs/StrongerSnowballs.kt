package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import ru.turbovadim.OriginSwapper.BooleanPDT
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility

class StrongerSnowballs : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "Snowballs you throw are packed with ice, and deal 1 damage!",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Stronger Snowballs",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key {
        return Key.key("moborigins:stronger_snowballs")
    }

    private val strongSnowballKey = NamespacedKey.fromString("strong-snowball")!!

    @EventHandler
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        if (event.entity.type != EntityType.SNOWBALL) return
        val player = event.entity.shooter as? Player ?: return
        runForAbility(player) {
            event.entity.persistentDataContainer.set(strongSnowballKey, BooleanPDT.BOOLEAN, true)
        }
    }

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val targetEntity = event.hitEntity as? LivingEntity ?: return
        val projectile = event.entity
        val isStrongSnowball = projectile.persistentDataContainer
            .get(strongSnowballKey, BooleanPDT.BOOLEAN) == true

        if (isStrongSnowball) {
            NMSInvoker.dealFreezeDamage(targetEntity, 1)
            val vector = projectile.velocity
            NMSInvoker.knockback(targetEntity, 0.5, -vector.x, -vector.z)
        }
    }
}
