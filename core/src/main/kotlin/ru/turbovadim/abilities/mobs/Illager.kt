package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import ru.turbovadim.abilities.types.Ability

class Illager : Ability, Listener {
    override fun getKey(): Key {
        return Key.key("moborigins:illager")
    }

    private val ILLAGERS = listOf(
        EntityType.ILLUSIONER,
        EntityType.EVOKER,
        EntityType.VINDICATOR,
        EntityType.RAVAGER,
        EntityType.VINDICATOR,
        EntityType.WITCH,
        EntityType.VEX,
        EntityType.PILLAGER
    )

    @EventHandler
    fun onEntityTargetLivingEntity(event: EntityTargetLivingEntityEvent) {
        if (ILLAGERS.contains(event.entity.type)) {
            val target = event.target ?: return
            runForAbility(target) {
                event.isCancelled = true
            }
        }
    }
}
