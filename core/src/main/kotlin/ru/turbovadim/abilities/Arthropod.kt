package ru.turbovadim.abilities

import net.kyori.adventure.key.Key
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.potion.PotionEffect
import ru.turbovadim.OriginsRebornEnhanced.Companion.NMSInvoker
import ru.turbovadim.abilities.Ability.AbilityRunner
import java.util.*

class Arthropod : Ability, Listener {
    override fun getKey(): Key {
        return Key.key("origins:arthropod")
    }

    private val random = Random()

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? LivingEntity ?: return
        val equipment = damager.equipment ?: return
        val mainHand = equipment.itemInMainHand
        val baneEnchantment = NMSInvoker.baneOfArthropodsEnchantment

        if (!mainHand.containsEnchantment(baneEnchantment)) return

        runForAbility(event.entity, AbilityRunner { player ->
            val level = mainHand.getEnchantmentLevel(baneEnchantment)
            event.damage += 1.25 * level
            val duration = (20 * random.nextDouble(1.0, 1 + (0.5 * level))).toInt()
            player.addPotionEffect(
                PotionEffect(
                    NMSInvoker.slownessEffect,
                    duration,
                    3,
                    false,
                    true
                )
            )
        })
    }

}
