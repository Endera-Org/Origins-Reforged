package ru.turbovadim.abilities

import net.kyori.adventure.key.Key
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Trident
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import ru.turbovadim.abilities.types.Ability
import ru.turbovadim.abilities.types.Ability.AbilityRunner

class Aquatic : Ability, Listener {
    override fun getKey(): Key {
        return Key.key("origins:aquatic")
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        runForAbility(event.entity, AbilityRunner { player ->
            val impalingLevel = when (val damager = event.damager) {
                is Trident -> damager.item.getEnchantmentLevel(Enchantment.IMPALING)
                is LivingEntity -> damager.equipment?.itemInMainHand?.getEnchantmentLevel(Enchantment.IMPALING)
                    ?: return@AbilityRunner
                else -> return@AbilityRunner
            }
            event.setDamage(event.damage + impalingLevel * 2.5)
        })
    }
}
