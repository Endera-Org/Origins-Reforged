package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.entity.EntityTameEvent
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility

class AlphaWolf : VisibleAbility, Listener {

    override val description: MutableList<OriginSwapper.LineData.LineComponent> =
        OriginSwapper.LineData.makeLineFor("Wolves you tame are stronger!", OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION)


    override val title: MutableList<OriginSwapper.LineData.LineComponent> =
        OriginSwapper.LineData.makeLineFor("Alpha Wolf", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override val key: Key = Key.key("moborigins:alpha_wolf")

    @EventHandler
    fun onEntityTame(event: EntityTameEvent) {
        if (event.getEntity().type == EntityType.WOLF) {
            val owner = event.owner as? Player ?: return
            runForAbility(owner) {
                makeWolfStronger(event.entity)
            }
        }
    }

    @EventHandler
    fun onEntityBreed(event: EntityBreedEvent) {
        if (event.getEntity().type == EntityType.WOLF) {
            if (event.breeder == null) return
            runForAbility(event.breeder as LivingEntity) {
                makeWolfStronger(event.entity)
            }
        }
    }

    private fun makeWolfStronger(wolf: LivingEntity) {
        val maxHealth = wolf.getAttribute(NMSInvoker.maxHealthAttribute)
        if (maxHealth != null) {
            maxHealth.baseValue = maxHealth.defaultValue + 10
        }
        val attackDamage = wolf.getAttribute(NMSInvoker.attackDamageAttribute)
        if (attackDamage != null) {
            attackDamage.baseValue = attackDamage.defaultValue + 2
        }
    }
}
