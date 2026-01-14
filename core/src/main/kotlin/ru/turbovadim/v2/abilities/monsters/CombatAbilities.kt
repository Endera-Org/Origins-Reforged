package ru.turbovadim.v2.abilities.monsters

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.Arrow
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.ability.DamageResult
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Combat-related abilities for monster origins.
 * Includes weapon abilities, bow/arrow abilities, and special attacks.
 */

// ============================================
// SPECIAL ATTACKS
// ============================================

/**
 * Explosive - create explosion by double-tapping sneak.
 * Has a 15-second cooldown, deals 8 damage to self.
 */
val explosive = ability("explosive", "monsterorigins") {
    title = text("Explosive")
    description("You can sacrifice some of your health to create an explosion every 15 seconds.")

    option("cooldown_ticks", 300) // 15 seconds
    option("explosion_power", 3.0f)
    option("self_damage", 8)
    option("double_sneak_window_ticks", 10)

    // Note: Full implementation requires tracking last sneak time per player
    // and cooldown management. Legacy behavior:
    // - Track lastToggledSneak map
    // - On sneak: check if within 10 ticks of last sneak
    // - If double-sneaked and no cooldown: create explosion, deal self damage, set cooldown
    //
    // This needs onSneak handler with state management:
    // val lastTick = lastToggledSneak.getOrDefault(player, Bukkit.getCurrentTick() - 11)
    // if (Bukkit.getCurrentTick() - lastTick <= 10) {
    //     player.location.createExplosion(player, 3f, false, true)
    //     NMSInvoker.dealExplosionDamage(player, 8)
    // }

    onSneak { player, sneaking, config ->
        if (!sneaking) return@onSneak

        // Note: State tracking and cooldown would be managed by AbilityManager
        // This is a simplified version - full implementation needs:
        // 1. Last sneak time tracking per player
        // 2. Cooldown checking/setting
        // 3. NMSInvoker for explosion damage

        val explosionPower = config.getFloat("explosion_power", 3.0f)
        val selfDamage = config.getInt("self_damage", 8)

        // Double-sneak detection would go here
        // For now, leaving as placeholder for proper state management
    }
}

/**
 * Sonic boom - ranged attack with empty hand.
 * Launches a sonic boom that damages entities in a line.
 */
val sonicBoom = ability("sonic_boom", "monsterorigins") {
    title = text("Sonic Boom")
    description("Every 30 seconds you can launch a sonic boom by hitting the air with your hand.")

    option("cooldown_ticks", 600) // 30 seconds
    option("damage", 15)
    option("range", 10)

    // Note: Full implementation requires PlayerLeftClickEvent (custom event)
    // and cooldown management. Legacy behavior:
    // - Check if main hand is empty
    // - Check/set cooldown
    // - Launch sonic boom: iterate 10 blocks in facing direction
    // - Spawn SONIC_BOOM particles
    // - Damage entities within 1 block radius (excluding self)
    // - Uses NMSInvoker.dealSonicBoomDamage

    onPrimaryAction { player, config ->
        if (player.inventory.itemInMainHand.type != Material.AIR) return@onPrimaryAction

        val damage = config.getInt("damage", 15)
        val range = config.getInt("range", 10)

        // Note: Cooldown check would be handled by AbilityManager
        val currentLoc = player.location.clone().add(0.0, 1.5, 0.0)
        val hitEntities = mutableListOf<LivingEntity>()

        for (i in 0 until range) {
            currentLoc.add(currentLoc.direction)
            currentLoc.world?.spawnParticle(Particle.SONIC_BOOM, currentLoc, 1)

            for (entity in currentLoc.getNearbyEntities(1.0, 1.0, 1.0)) {
                if (entity === player) continue
                if (entity !is LivingEntity) continue
                if (entity in hitEntities) continue

                hitEntities.add(entity)
                // Note: Would use NMSInvoker.dealSonicBoomDamage(entity, damage, player)
                // For now, using regular damage
                entity.damage(damage.toDouble(), player)
            }
        }
    }
}

// ============================================
// BOW/ARROW ABILITIES
// ============================================

/**
 * Infinite arrows - arrows are not consumed when shot.
 * Note: Requires EntityShootBowEvent handling.
 */
val infiniteArrows = ability("infinite_arrows", "monsterorigins") {
    title = text("Infinite Arrows")
    description("Arrows you shoot are not used up.")

    // Note: Full implementation requires EntityShootBowEvent handling
    // Legacy behavior:
    // val arrow = event.consumable
    // if (arrow?.type == Material.ARROW) {
    //     player.inventory.addItem(arrow)
    // }
    //
    // This gives back the arrow to the player's inventory after shooting
}

/**
 * Slowness arrows - arrows apply slowness effect on hit.
 * Note: Requires EntityShootBowEvent handling.
 */
val slownessArrows = ability("slowness_arrows", "monsterorigins") {
    title = text("Frozen Arrows")
    description("All arrows you shoot have the slowness effect.")

    option("slowness_duration", 600)
    option("slowness_amplifier", 0)

    // Note: Full implementation requires EntityShootBowEvent handling
    // Legacy behavior:
    // val arrow = event.projectile as? Arrow
    // arrow?.addCustomEffect(
    //     PotionEffect(PotionEffectType.SLOWNESS, 600, 0, false, true),
    //     false
    // )
}

/**
 * Better aim - more accurate and faster arrow shots.
 * Note: Requires EntityShootBowEvent handling with NMS arrow launching.
 */
val betterAim = ability("better_aim", "monsterorigins") {
    title = text("Sniper")
    description("Your aim is more accurate than humans.")

    option("velocity_multiplier", 3.0f)

    // Note: Full implementation requires EntityShootBowEvent handling
    // Legacy behavior: NMSInvoker.launchArrow(event.projectile, event.entity, 0f, 3 * event.force, 0f)
    // This modifies the arrow's velocity to be 3x faster with no spread
}

// ============================================
// TRIDENT ABILITIES
// ============================================

/**
 * Trident expert - master of the trident weapon.
 * Bonus damage, channeling without thunder, riptide without water.
 */
val tridentExpert = ability("trident_expert", "moborigins") {
    title = text("Trident Expert")
    description(
        "You're a master of the trident, dealing +2 damage when you throw it,",
        "and +2 melee damage with it. You can also use channeling without thunder,",
        "and use riptide without rain/water at the price of extra durability."
    )

    option("bonus_damage", 2.0)
    option("riptide_durability_cost", 10)
    option("activation_cooldown_ticks", 400)

    // Note: Full implementation requires multiple event handlers:
    // 1. EntityDamageByEntityEvent for bonus damage with trident
    // 2. ProjectileLaunchEvent for thrown trident bonus
    // 3. PlayerRiptideEvent for riptide without water
    // 4. Custom handling for channeling without thunder
    //
    // This is a complex ability that needs custom event handling beyond the DSL
}

// ============================================
// WATER COMBAT
// ============================================

/**
 * Water combatant - deal more damage while in water.
 */
val waterCombatant = ability("water_combatant", "moborigins") {
    title = text("Water Combatant")
    description("You deal more damage while in water.")

    option("bonus_damage", 3.0)

    modifyDamage(
        outgoing = { player, damage, _, config ->
            if (player.isInWater) {
                val bonus = config.getDouble("bonus_damage", 3.0)
                DamageResult.Modify(damage + bonus)
            } else {
                DamageResult.Allow
            }
        }
    )
}

// ============================================
// VILLAGER INTERACTIONS
// ============================================

/**
 * Zombie touch - zombify villagers on kill instead of killing them.
 * Note: Requires EntityDeathEvent handling.
 */
val zombieTouch = ability("zombie_touch", "monsterorigins") {
    title = text("Zombie Touch")
    description("You zombify villagers instead of killing them.")

    // Note: Full implementation requires EntityDeathEvent handling
    // Legacy behavior:
    // val villager = event.entity as? Villager
    // if (villager != null && event.entity.killer != null) {
    //     event.isCancelled = true
    //     villager.zombify()
    // }
}

/**
 * Scare villagers - villagers flee from player and refuse to trade.
 * Note: Requires multiple event handlers and NMS for mob goals.
 */
val scareVillagers = ability("scare_villagers", "monsterorigins") {
    title = text("Terrifying Monster")
    description("Villagers are scared of you and refuse to trade with you.")

    // Note: Full implementation requires:
    // 1. EntitySpawnEvent/EntitiesLoadEvent to add fear goal to villagers
    // 2. PlayerInteractEntityEvent to cancel trading and make villager shake head
    // 3. NMSInvoker.getVillagerAfraidGoal for the mob AI goal
    //
    // Legacy behavior adds a custom mob goal that makes villagers flee from
    // players with this ability, and cancels interaction events to prevent trading
}
