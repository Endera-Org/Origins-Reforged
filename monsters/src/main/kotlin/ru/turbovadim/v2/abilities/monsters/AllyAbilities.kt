package ru.turbovadim.v2.abilities.monsters

import io.papermc.paper.tag.EntityTags
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.PigZombie
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import java.util.UUID
import java.util.WeakHashMap
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text

/**
 * Ally abilities for monster origins.
 */

private val undeadAttackedBy: MutableMap<UUID, MutableSet<UUID>> = WeakHashMap()
private val piglinAttackedBy: MutableMap<UUID, MutableSet<UUID>> = WeakHashMap()

val creeperAlly = ability("creeper_ally", "monsterorigins") {
    title = text("Creeper Ally")
    description("Creepers don't attack you!")

    onEntityTarget { _, attacker, _ ->
        attacker.type != EntityType.CREEPER
    }
}

val undeadAllyMonsters = ability("undead_ally", "monsterorigins") {
    title = text("Undead Ally")
    description("Undead mobs don't attack you, unless you attack them first.")

    onEntityTarget { player, attacker, _ ->
        if (!EntityTags.UNDEADS.isTagged(attacker.type)) return@onEntityTarget true
        val attacked = undeadAttackedBy[player.uniqueId]
        attacked != null && attacker.uniqueId in attacked
    }

    listener<EntityDamageByEntityEvent>(
        ignoreCancelled = false,
        playerFrom = { event ->
            when (val damager = event.damager) {
                is Player -> damager
                is Projectile -> damager.shooter as? Player
                else -> null
            }
        }
    ) { player, event, _ ->
        undeadAttackedBy.getOrPut(player.uniqueId) { HashSet() }.add(event.entity.uniqueId)
    }
}

val guardianAllyMonsters = ability("guardian_ally", "monsterorigins") {
    title = text("Guardian Ally")
    description("Guardians don't attack you!")

    onEntityTarget { _, attacker, _ ->
        attacker.type != EntityType.GUARDIAN && attacker.type != EntityType.ELDER_GUARDIAN
    }
}

val piglinAlly = ability("piglin_ally", "monsterorigins") {
    title = text("Piglin Ally")
    description("Piglins don't attack you, unless you attack them first.")

    onEntityTarget { player, attacker, _ ->
        if (attacker.type != EntityType.PIGLIN && attacker.type != EntityType.PIGLIN_BRUTE) {
            return@onEntityTarget true
        }
        val attacked = piglinAttackedBy[player.uniqueId]
        attacked != null && attacker.uniqueId in attacked
    }

    listener<EntityDamageByEntityEvent>(
        ignoreCancelled = false,
        playerFrom = { event ->
            when (val damager = event.damager) {
                is Player -> damager
                is Projectile -> damager.shooter as? Player
                else -> null
            }
        }
    ) { player, event, _ ->
        piglinAttackedBy.getOrPut(player.uniqueId) { HashSet() }.add(event.entity.uniqueId)
    }
}

val zombifiedPiglinAllies = ability("zombified_piglin_allies", "monsterorigins") {
    title = text("Terrifying Armies")
    description("Nearby Zombified Piglins will attack anything that attacks you or that you attack.")

    option("ally_radius", 32.0)

    onAttack { player, target, config ->
        if (target !is LivingEntity) return@onAttack
        val radius = config.getDouble("ally_radius", 32.0)
        angerNearbyPigZombies(player, target, radius)
    }

    listener<EntityDamageByEntityEvent>(
        ignoreCancelled = false,
        playerFrom = { it.entity as? Player }
    ) { player, event, config ->
        val source: LivingEntity = when (val damager = event.damager) {
            is LivingEntity -> damager
            is Projectile -> damager.shooter as? LivingEntity ?: return@listener
            else -> return@listener
        }
        if (source === player) return@listener
        val radius = config.getDouble("ally_radius", 32.0)
        angerNearbyPigZombies(player, source, radius)
    }
}

private fun angerNearbyPigZombies(origin: Entity, targetEntity: LivingEntity, radius: Double) {
    origin.getNearbyEntities(radius, radius, radius)
        .filter { it.type == EntityType.ZOMBIFIED_PIGLIN }
        .forEach { entity ->
            (entity as PigZombie).apply {
                isAngry = true
                target = targetEntity
            }
        }
}
