package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPotionEffectEvent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.Ability

class MiningFatigueImmune : Ability, Listener {
    override fun getKey(): Key {
        return Key.key("moborigins:mining_fatigue_immune")
    }

    @EventHandler
    fun onEntityPotionEffect(event: EntityPotionEffectEvent) {
        runForAbility(event.entity) {
            event.newEffect?.let { effect ->
                if (effect.type == NMSInvoker.miningFatigueEffect) {
                    event.isCancelled = true
                }
            }
        }
    }
}
