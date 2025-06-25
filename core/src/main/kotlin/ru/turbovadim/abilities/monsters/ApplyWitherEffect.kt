package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginSwapper
import ru.turbovadim.abilities.types.VisibleAbility

class ApplyWitherEffect : VisibleAbility, Listener {

    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "Anything you hit gets the Wither effect.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor("Wither", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override fun getKey(): Key = Key.key("monsterorigins:apply_wither_effect")

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val entity = event.entity as? LivingEntity
        if (entity != null) {
            runForAbility(event.damager) {
                entity.addPotionEffect(PotionEffect(PotionEffectType.WITHER, 200, 0, false, true))
            }
        }
    }
}