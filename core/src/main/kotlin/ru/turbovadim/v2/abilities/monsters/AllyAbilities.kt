package ru.turbovadim.v2.abilities.monsters

import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.PigZombie
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Ally abilities for monster origins.
 * These abilities make certain mobs passive towards the player.
 */

// ============================================
// CREEPER ALLY
// ============================================

/**
 * Creepers don't attack you.
 * Note: Requires EntityTargetLivingEntityEvent handling.
 */
val creeperAlly = ability("creeper_ally", "monsterorigins") {
    title = text("Creeper Ally")
    description("Creepers don't attack you!")

    // Note: Full implementation requires EntityTargetLivingEntityEvent handling
    // Legacy behavior:
    // if (event.entityType != EntityType.CREEPER) return
    // val target = event.target ?: return
    // runForAbility(target) { event.isCancelled = true }
    //
    // This cancels the targeting event when a creeper tries to target
    // a player with this ability
}

// ============================================
// UNDEAD ALLIES
// ============================================

/**
 * Undead mobs don't attack unless provoked.
 * Tracks which entities the player has attacked and only allows
 * undead mobs to target the player if the player attacked them first.
 * Note: Requires EntityTargetLivingEntityEvent and EntityDamageByEntityEvent handling.
 */
val undeadAllyMonsters = ability("undead_ally", "monsterorigins") {
    title = text("Undead Ally")
    description("Undead mobs don't attack you, unless you attack them first.")

    // Note: Full implementation requires:
    // 1. EntityTargetLivingEntityEvent to cancel targeting unless player attacked first
    // 2. EntityDamageByEntityEvent to track player attacks
    // 3. State management for attackedEntities map
    //
    // Legacy behavior uses EntityTags.UNDEADS.isTagged(event.entityType) to check
    // if the targeting entity is undead, and maintains a map of which entities
    // each player has attacked:
    //
    // attackedEntities: MutableMap<Player, MutableList<Entity>>
    //
    // On target event:
    // if (EntityTags.UNDEADS.isTagged(event.entityType)) {
    //     val player = event.target as? Player
    //     if (player != null) {
    //         if (!attackedEntities.getOrDefault(player, emptyList()).contains(event.entity)) {
    //             event.isCancelled = true
    //         }
    //     }
    // }
    //
    // On damage event:
    // val player = when (val damager = event.damager) {
    //     is Player -> damager
    //     is Projectile -> damager.shooter as? Player
    //     else -> null
    // }
    // if (player != null) {
    //     attackedEntities.getOrPut(player) { mutableListOf() }.add(event.entity)
    // }
}

// ============================================
// GUARDIAN ALLY
// ============================================

/**
 * Guardians don't attack you.
 * Note: Requires EntityTargetLivingEntityEvent handling.
 */
val guardianAllyMonsters = ability("guardian_ally", "monsterorigins") {
    title = text("Guardian Ally")
    description("Guardians don't attack you!")

    // Note: Full implementation requires EntityTargetLivingEntityEvent handling
    // Legacy behavior:
    // if (event.entityType == EntityType.GUARDIAN || event.entityType == EntityType.ELDER_GUARDIAN) {
    //     val target = event.target ?: return
    //     runForAbility(target) { event.isCancelled = true }
    // }
}

// ============================================
// PIGLIN ALLIES
// ============================================

/**
 * Piglins don't attack unless provoked.
 * Similar to undead ally but specifically for Piglins and Piglin Brutes.
 * Note: Requires EntityTargetLivingEntityEvent and EntityDamageByEntityEvent handling.
 */
val piglinAlly = ability("piglin_ally", "monsterorigins") {
    title = text("Piglin Ally")
    description("Piglins don't attack you, unless you attack them first.")

    // Note: Full implementation requires:
    // 1. EntityTargetLivingEntityEvent to cancel targeting unless player attacked first
    // 2. EntityDamageByEntityEvent to track player attacks
    // 3. State management for attackedEntities map
    //
    // Legacy behavior:
    // On target event:
    // if (event.entityType == EntityType.PIGLIN || event.entityType == EntityType.PIGLIN_BRUTE) {
    //     val target = event.target as? Player ?: return
    //     val alreadyAttacked = attackedEntities[target]?.contains(event.entity) ?: false
    //     if (!alreadyAttacked) {
    //         event.isCancelled = true
    //     }
    // }
    //
    // On damage event (same as undead ally):
    // Track when player attacks piglins to allow retaliation
}

/**
 * Zombified Piglins fight alongside you.
 * When you attack or are attacked, nearby Zombified Piglins become angry
 * and target your attacker/target.
 * Note: Requires EntityDamageByEntityEvent handling.
 */
val zombifiedPiglinAllies = ability("zombified_piglin_allies", "monsterorigins") {
    title = text("Terrifying Armies")
    description("Nearby Zombified Piglins will attack anything that attacks you or that you attack.")

    option("ally_radius", 32.0)

    // Note: Full implementation requires EntityDamageByEntityEvent handling
    // Legacy behavior handles two cases:
    //
    // 1. When player attacks something:
    // val target = event.entity
    // if (target is LivingEntity) {
    //     val player = when (val damager = event.damager) {
    //         is Player -> damager
    //         is Projectile -> damager.shooter as? Player
    //         else -> null
    //     }
    //     if (player != null) {
    //         player.getNearbyEntities(32.0, 32.0, 32.0)
    //             .filter { it.type == EntityType.ZOMBIFIED_PIGLIN }
    //             .forEach { z ->
    //                 (z as PigZombie).apply {
    //                     isAngry = true
    //                     target = target
    //                 }
    //             }
    //     }
    // }
    //
    // 2. When player is attacked:
    // val source = when (val damager = event.damager) {
    //     is LivingEntity -> damager
    //     is Projectile -> damager.shooter as? LivingEntity
    //     else -> null
    // }
    // if (source != null) {
    //     event.entity.getNearbyEntities(32.0, 32.0, 32.0)
    //         .filter { it.type == EntityType.ZOMBIFIED_PIGLIN }
    //         .forEach { z ->
    //             (z as PigZombie).apply {
    //                 isAngry = true
    //                 target = source
    //             }
    //         }
    // }

    // Partial implementation using onAttack for when player attacks
    onAttack { player, target, config ->
        if (target !is LivingEntity) return@onAttack

        val radius = config.getDouble("ally_radius", 32.0)
        player.getNearbyEntities(radius, radius, radius)
            .filter { it.type == EntityType.ZOMBIFIED_PIGLIN }
            .forEach { entity ->
                (entity as PigZombie).apply {
                    isAngry = true
                    this.target = target
                }
            }
    }

    // Note: The defensive case (when player is attacked) requires
    // additional EntityDamageByEntityEvent handling where the player
    // is the victim, not handled by onAttack
}
