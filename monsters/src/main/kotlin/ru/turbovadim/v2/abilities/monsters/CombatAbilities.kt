package ru.turbovadim.v2.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.entity.Arrow
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.Villager
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.ability.DamageResult
import ru.turbovadim.v2.api.OriginsApi
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import java.util.UUID
import java.util.WeakHashMap

/**
 * Combat-related abilities for monster origins.
 */

private val lastSneakTick: MutableMap<UUID, Int> = HashMap()
private val treatedVillagers: MutableSet<UUID> = WeakHashMap<UUID, Boolean>().keys

private val explosiveKey = Key.key("monsterorigins", "explosive")
private val sonicBoomKey = Key.key("monsterorigins", "sonic_boom")

val explosive = ability("explosive", "monsterorigins") {
    title = text("Explosive")
    description("You can sacrifice some of your health to create an explosion every 15 seconds.")

    option("cooldown_ticks", 300)
    option("explosion_power", 3.0)
    option("self_damage", 8)
    option("double_sneak_window_ticks", 10)

    onSneak { player, sneaking, config ->
        if (!sneaking) return@onSneak
        val api = OriginsApi.getOrNull()
        if (api != null && api.hasCooldown(player, explosiveKey)) return@onSneak

        val currentTick = Bukkit.getCurrentTick()
        val window = config.getInt("double_sneak_window_ticks", 10)
        val lastTick = lastSneakTick[player.uniqueId] ?: (currentTick - window - 1)

        if (currentTick - lastTick <= window) {
            lastSneakTick.remove(player.uniqueId)
            val power = config.getDouble("explosion_power", 3.0).toFloat()
            val selfDamage = config.getInt("self_damage", 8)
            player.location.createExplosion(player, power, false, true)
            OriginsReforged.NMSInvoker.dealExplosionDamage(player, selfDamage)
            api?.setCooldown(player, explosiveKey, config.getInt("cooldown_ticks", 300), "tnt")
        } else {
            lastSneakTick[player.uniqueId] = currentTick
        }
    }
}

val sonicBoom = ability("sonic_boom", "monsterorigins") {
    title = text("Sonic Boom")
    description("Every 30 seconds you can launch a sonic boom by hitting the air with your hand.")

    option("cooldown_ticks", 600)
    option("damage", 15)
    option("range", 10)

    onPrimaryAction { player, config ->
        if (player.inventory.itemInMainHand.type != Material.AIR) return@onPrimaryAction
        val api = OriginsApi.getOrNull()
        if (api != null && api.hasCooldown(player, sonicBoomKey)) return@onPrimaryAction

        val damage = config.getInt("damage", 15)
        val range = config.getInt("range", 10)
        val currentLoc = player.location.clone().add(0.0, 1.5, 0.0)
        val hitEntities = HashSet<UUID>()

        for (i in 0 until range) {
            currentLoc.add(currentLoc.direction)
            currentLoc.world.spawnParticle(Particle.SONIC_BOOM, currentLoc, 1)

            for (entity in currentLoc.getNearbyEntities(1.0, 1.0, 1.0)) {
                if (entity === player) continue
                if (entity !is LivingEntity) continue
                if (!hitEntities.add(entity.uniqueId)) continue
                OriginsReforged.NMSInvoker.dealSonicBoomDamage(entity, damage, player)
            }
        }

        api?.setCooldown(player, sonicBoomKey, config.getInt("cooldown_ticks", 600), "echo_shard")
    }
}

val infiniteArrows = ability("infinite_arrows", "monsterorigins") {
    title = text("Infinite Arrows")
    description("Arrows you shoot are not used up.")

    listener<EntityShootBowEvent>(
        ignoreCancelled = false,
        playerFrom = { it.entity as? Player }
    ) { player, event, _ ->
        val arrow = event.consumable ?: return@listener
        if (arrow.type != Material.ARROW) return@listener
        player.inventory.addItem(arrow)
    }
}

val slownessArrows = ability("slowness_arrows", "monsterorigins") {
    title = text("Frozen Arrows")
    description("All arrows you shoot have the slowness effect.")

    option("slowness_duration", 600)
    option("slowness_amplifier", 0)

    listener<EntityShootBowEvent>(
        ignoreCancelled = false,
        playerFrom = { it.entity as? Player }
    ) { _, event, config ->
        val arrow = event.projectile as? Arrow ?: return@listener
        val duration = config.getInt("slowness_duration", 600)
        val amplifier = config.getInt("slowness_amplifier", 0)
        arrow.addCustomEffect(
            PotionEffect(
                OriginsReforged.NMSInvoker.slownessEffect,
                duration,
                amplifier,
                false,
                true
            ),
            false
        )
    }
}

val betterAim = ability("better_aim", "monsterorigins") {
    title = text("Sniper")
    description("Your aim is more accurate than humans.")

    option("velocity_multiplier", 3.0f)

    listener<EntityShootBowEvent>(
        ignoreCancelled = false,
        playerFrom = { it.entity as? Player }
    ) { _, event, config ->
        val multiplier = config.getFloat("velocity_multiplier", 3.0f)
        OriginsReforged.NMSInvoker.launchArrow(event.projectile, event.entity, 0f, multiplier * event.force, 0f)
    }
}

val tridentExpert = ability("trident_expert", "moborigins") {
    title = text("Trident Expert")
    description(
        "You're a master of the trident, dealing +2 damage when you throw it,",
        "and +2 melee damage with it."
    )

    option("bonus_damage", 2.0)

    modifyDamage(
        outgoing = { player, damage, _, config ->
            val item = player.inventory.itemInMainHand
            if (item.type == Material.TRIDENT) {
                val bonus = config.getDouble("bonus_damage", 2.0)
                DamageResult.Modify(damage + bonus)
            } else {
                DamageResult.Allow
            }
        }
    )

    listener<EntityDamageByEntityEvent>(
        ignoreCancelled = true,
        playerFrom = { event ->
            val projectile = event.damager as? Projectile ?: return@listener null
            if (projectile.type != EntityType.TRIDENT) return@listener null
            projectile.shooter as? Player
        }
    ) { _, event, config ->
        val bonus = config.getDouble("bonus_damage", 2.0)
        event.damage += bonus
    }
}

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

val zombieTouch = ability("zombie_touch", "monsterorigins") {
    title = text("Zombie Touch")
    description("You zombify villagers instead of killing them.")

    listener<EntityDeathEvent>(
        ignoreCancelled = false,
        playerFrom = { event ->
            val villager = event.entity as? Villager ?: return@listener null
            villager.killer
        }
    ) { _, event, _ ->
        val villager = event.entity as? Villager ?: return@listener
        event.isCancelled = true
        villager.zombify()
    }
}

private val hitByPlayerKey: NamespacedKey by lazy {
    NamespacedKey(OriginsReforged.instance, "hit-by-player")
}

val scareVillagers = ability("scare_villagers", "monsterorigins") {
    title = text("Terrifying Monster")
    description("Villagers are scared of you and refuse to trade with you.")

    option("goal_radius", 24.0)

    onTick(interval = 20) { player, config ->
        val api = OriginsApi.getOrNull() ?: return@onTick true
        val abilityKey = Key.key("monsterorigins", "scare_villagers")
        val radius = config.getDouble("goal_radius", 24.0)

        player.getNearbyEntities(radius, radius, radius).forEach { entity ->
            val villager = entity as? Villager ?: return@forEach
            if (treatedVillagers.add(villager.uniqueId)) {
                Bukkit.getMobGoals().addGoal(
                    villager,
                    0,
                    OriginsReforged.NMSInvoker.getVillagerAfraidGoal(villager) { nearby ->
                        api.hasAbility(nearby, abilityKey)
                    }
                )
            }
        }
        true
    }

    listener<PlayerInteractEntityEvent>(
        ignoreCancelled = true,
        playerFrom = { it.player }
    ) { _, event, _ ->
        val villager = event.rightClicked as? Villager ?: return@listener
        event.isCancelled = true
        villager.shakeHead()
    }

    listener<EntityDamageByEntityEvent>(
        ignoreCancelled = true,
        playerFrom = { event ->
            if (event.entity.type != EntityType.CREEPER) return@listener null
            when (val damager = event.damager) {
                is Player -> damager
                is Projectile -> damager.shooter as? Player
                else -> null
            }
        }
    ) { player, event, _ ->
        event.entity.persistentDataContainer.set(
            hitByPlayerKey,
            PersistentDataType.STRING,
            player.name
        )
    }
}
