package ru.turbovadim.abilities.mobs

import io.papermc.paper.event.entity.EntityMoveEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.persistence.PersistentDataType
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.OriginsReforged
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.cooldowns.CooldownAbility
import ru.turbovadim.cooldowns.Cooldowns.CooldownInfo
import ru.turbovadim.events.PlayerLeftClickEvent
import java.util.*

class Split : VisibleAbility, Listener, CooldownAbility {

    override val description: MutableList<LineComponent> =
        LineData.makeLineFor("Turn your food points into a small slime to defend you!", LineType.DESCRIPTION)

    override val title: MutableList<LineComponent> = LineData.makeLineFor("Split Ability", LineType.TITLE)

    override val key: Key = Key.key("moborigins:split")

    @EventHandler
    fun onPlayerLeftClick(event: PlayerLeftClickEvent) {
        if (event.item != null) return

        val player = event.player
        if (player.foodLevel < 8) return

        runForAbility(player) {
            if (hasCooldown(player)) return@runForAbility
            setCooldown(player)
            player.foodLevel -= 8

            val slime = player.world.spawnEntity(player.location, EntityType.SLIME) as Slime
            slime.size = 2
            slime.persistentDataContainer.set(slimeKey, PersistentDataType.STRING, player.uniqueId.toString())
        }
    }

    private fun getPlayerFromSlime(slime: Entity): Player? {
        val s = slime.persistentDataContainer.get(slimeKey, PersistentDataType.STRING) ?: return null
        return Bukkit.getPlayer(UUID.fromString(s))
    }

    private val slimeKey = NamespacedKey(OriginsReforged.instance, "slime-target")

    @EventHandler
    fun onEntityTargetLivingEntity(event: EntityTargetLivingEntityEvent) {
        if (event.reason == EntityTargetEvent.TargetReason.CUSTOM) return

        val entity = event.entity
        if (!entity.persistentDataContainer.has(slimeKey)) return

        val s = entity.persistentDataContainer.get(slimeKey, PersistentDataType.STRING) ?: return
        val target = event.target ?: return

        val offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(s))
        if (offlinePlayer.uniqueId == target.uniqueId) {
            val slimePlayer = getPlayerFromSlime(entity)
            val lastHit = lastHitEntity[slimePlayer]
            event.setTarget(lastHit)
            targetEntity[entity] = lastHit
        }
    }

    var lastHitEntity = mutableMapOf<Player?, LivingEntity?>()
    var targetEntity = mutableMapOf<Entity?, LivingEntity?>()

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (event.damager.persistentDataContainer.has(slimeKey)) {
            val playerFromSlime = getPlayerFromSlime(event.damager) ?: return
            if (event.entity.uniqueId == playerFromSlime.uniqueId) {
                event.isCancelled = true
            }
        }

        val player: Player = when (val damager = event.damager) {
            is Player -> damager
            is Projectile -> {
                val shooter = damager.shooter
                shooter as? Player ?: return
            }
            else -> return
        }

        if (event.entity is LivingEntity) {
            lastHitEntity[player] = event.entity as LivingEntity?
        }
    }

    @EventHandler
    fun onEntityMove(event: EntityMoveEvent) {
        val slime = event.entity
        if (slime.type != EntityType.SLIME || !slime.persistentDataContainer.has(slimeKey)) return

        targetEntity[slime]?.let { target ->
            if (target.location.distance(slime.location) <= 1) {
                slime.attack(target)
            }
        }
    }

    override val cooldownInfo: CooldownInfo
        get() = CooldownInfo(600, "split")
}
