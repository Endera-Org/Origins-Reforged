package ru.turbovadim.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Creeper
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.world.EntitiesLoadEvent
import org.bukkit.persistence.PersistentDataType
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.OriginsReforged.Companion.instance
import ru.turbovadim.abilities.types.Ability.AbilityRunner
import ru.turbovadim.abilities.types.VisibleAbility
import java.util.function.Predicate

class ScareCreepers : VisibleAbility, Listener {

    override fun getKey(): Key {
        return Key.key("origins:scare_creepers")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "Creepers are scared of you and will only explode if you attack them first.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor("Catlike Appearance", LineComponent.LineType.TITLE)

    @EventHandler
    fun onEntitySpawn(event: EntitySpawnEvent) {
        val creeper = event.entity
        if (creeper is Creeper) {
            fixCreeper(creeper)
        }
    }

    @EventHandler
    fun onEntitiesLoad(event: EntitiesLoadEvent) {
        for (entity in event.entities) {
            if (entity is Creeper) {
                fixCreeper(entity)
            }
        }
    }

    fun fixCreeper(creeper: Creeper) {
        val afraidGoal = NMSInvoker.getCreeperAfraidGoal(
            creeper,
            { player -> hasAbility(player) },
            { livingEntity ->
                creeper.persistentDataContainer.get(hitByPlayerKey, PersistentDataType.STRING)
                    ?.let { stored -> stored == livingEntity.uniqueId.toString() } == true
            }
        )
        Bukkit.getMobGoals().addGoal(creeper, 0, afraidGoal)
    }

    private val hitByPlayerKey = NamespacedKey(instance, "hit-by-player")

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (event.entity.type != EntityType.CREEPER) return

        val player: Player = when (val damager = event.damager) {
            is Projectile -> {
                val shooter = damager.shooter
                shooter as? Player ?: return
            }
            is Player -> damager
            else -> return
        }

        runForAbility(player) { p ->
            p.persistentDataContainer.set(hitByPlayerKey, PersistentDataType.STRING, p.uniqueId.toString())
        }
    }


    @EventHandler
    fun onEntityTargetLivingEntity(event: EntityTargetLivingEntityEvent) {
        if (event.entity.type == EntityType.CREEPER) {
            val target = event.target
            if (target == null) return
            runForAbility(target, AbilityRunner { player: Player? ->
                val data = event.entity.persistentDataContainer
                    .get(hitByPlayerKey, PersistentDataType.STRING)
                if (data == null) {
                    event.isCancelled = true
                    return@AbilityRunner
                }
                if (data != player!!.uniqueId.toString()) {
                    event.isCancelled = true
                }
            })
        }
    }
}
