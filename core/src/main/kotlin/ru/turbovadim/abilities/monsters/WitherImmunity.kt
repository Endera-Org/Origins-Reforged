package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.abilities.types.Ability

class WitherImmunity : Ability, Listener {
    override val key: Key
        get() {
            return Key.key("monsterorigins:wither_immunity")
        }

    @EventHandler
    fun onEntityPotionEffect(event: EntityPotionEffectEvent) {
        runForAbility(event.getEntity()) {
            if (event.newEffect != null) {
                if (event.newEffect!!.type == PotionEffectType.WITHER) {
                    event.isCancelled = true
                }
            }
        }
    }
}
