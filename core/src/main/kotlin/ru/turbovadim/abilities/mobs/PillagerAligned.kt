package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.entity.IronGolem
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.world.EntitiesLoadEvent
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility

class PillagerAligned : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> =
        LineData.makeLineFor("Villagers don't like you, and pillagers like you!", LineType.DESCRIPTION)

    override val title: MutableList<LineComponent> = LineData.makeLineFor("Pillager Aligned", LineType.TITLE)

    override val key: Key = Key.key("moborigins:pillager_aligned")

    @EventHandler
    fun onEntityTargetLivingEntity(event: EntityTargetLivingEntityEvent) {
        if (event.entity.type == EntityType.PILLAGER) {
            val target = event.target ?: return
            runForAbility(target) { event.isCancelled = true }
        }
    }

    @EventHandler
    fun onPlayerInteractAtEntity(event: PlayerInteractEntityEvent) {
        val villager = event.rightClicked as? Villager ?: return
        runForAbility(event.player) {
            event.isCancelled = true
            villager.shakeHead()
        }
    }

    @EventHandler
    fun onEntitySpawn(event: EntitySpawnEvent) {
        val golem = event.entity as? IronGolem ?: return
        fixGolem(golem)
    }

    @EventHandler
    fun onEntitiesLoad(event: EntitiesLoadEvent) {
        for (entity in event.entities) {
            if (entity is IronGolem) {
                fixGolem(entity)
            }
        }
    }

    fun fixGolem(golem: IronGolem) {
        val goal = NMSInvoker.getIronGolemAttackGoal(golem) { player ->
            hasAbility(player)
        }
        Bukkit.getMobGoals().addGoal(golem, 3, goal)
    }
}
