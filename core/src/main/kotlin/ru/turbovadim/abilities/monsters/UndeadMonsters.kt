package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.Ability

class UndeadMonsters : Ability, Listener {
    override val key: Key = Key.key("monsterorigins:undead")

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        runForAbility(event.getEntity()) {
            val entity = event.getEntity() as? LivingEntity
            if (entity != null) {
                val level: Int =
                    entity.activeItem.getEnchantmentLevel(NMSInvoker.getSmiteEnchantment())
                event.setDamage(event.damage + (2.5 * level))
            }
        }
    }
}
