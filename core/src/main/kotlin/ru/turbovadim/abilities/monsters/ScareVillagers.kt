package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.world.EntitiesLoadEvent
import org.bukkit.persistence.PersistentDataType
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.OriginsReforged.Companion.instance
import ru.turbovadim.abilities.types.VisibleAbility

class ScareVillagers : VisibleAbility, Listener {
    override val key: Key = Key.key("monsterorigins:scare_villagers")

    override val description: MutableList<LineComponent> = makeLineFor(
        "Villagers are scared of you and refuse to trade with you.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Terrifying Monster",
        LineComponent.LineType.TITLE
    )

    @EventHandler
    fun onEntitySpawn(event: EntitySpawnEvent) {
        val villager = event.entity as? Villager ?: return
        fixVillager(villager)
    }

    @EventHandler
    fun onEntitiesLoad(event: EntitiesLoadEvent) {
        for (entity in event.entities) {
            val villager = entity as? Villager ?: continue
            fixVillager(villager)
        }
    }

    fun fixVillager(villager: Villager) {
        Bukkit.getMobGoals().addGoal(
            villager, 0,
            NMSInvoker.getVillagerAfraidGoal(villager) { player: Player ->
                hasAbility(player)
            }
        )
    }

    private val hitByPlayerKey: NamespacedKey = NamespacedKey(instance, "hit-by-player")

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (event.entity.type == EntityType.CREEPER) {
            val damager = event.damager
            val player: Player? = when (damager) {
                is Player -> damager
                is Projectile -> damager.shooter as? Player
                else -> null
            }
            if (player != null) {
                runForAbility(player) {
                    event.entity.persistentDataContainer.set(hitByPlayerKey, PersistentDataType.STRING, player.name)
                }
            }
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
}
