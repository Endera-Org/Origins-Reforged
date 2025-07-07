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

class ApplyHungerEffect : VisibleAbility, Listener {

    override val description: MutableList<OriginSwapper.LineData.LineComponent> = OriginSwapper.LineData.makeLineFor(
        "Anything you hit gets the Hunger effect.",
        OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<OriginSwapper.LineData.LineComponent> =
        OriginSwapper.LineData.makeLineFor("Hunger", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override val key: Key = Key.key("monsterorigins:apply_hunger_effect")

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val entity = event.entity as? LivingEntity
        if (entity != null) {
            runForAbility(event.damager) {
                entity.addPotionEffect(PotionEffect(PotionEffectType.HUNGER, 200, 0, false, true))
            }
        }
    }
}
