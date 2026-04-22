package ru.turbovadim.v2.abilities.magic

import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes
import io.papermc.paper.tag.EntityTags
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.endera.enderalib.utils.async.runTask
import org.endera.enderalib.utils.async.runTaskLater
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.api.OriginsApi
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap

/**
 * Necromancer origin abilities ported from Origins-Magic (StarshooterCity).
 *
 * Includes:
 *   - undead_commander: Nearby undead retarget whatever you attack.
 *   - bring_back_dead: Left-click empty hand to resurrect nearby dead players.
 *   - spirit_strength: Stronger + more HP in the Nether, weaker elsewhere (multi).
 *   - undead_ally: Undead mobs don't target you unless you provoked them.
 *   - dark_aura: Villagers refuse to trade and soul particles trail you.
 */

/**
 * Lord of the Dead - Damaging an entity forces every nearby undead (16 block radius)
 * to retarget that entity. 30 second cooldown.
 */
val undeadCommander = ability("undead_commander", "magicorigins") {
    title = text("Lord of the Dead")
    description("Nearby undead monsters not targeting you will go after whatever you attack.")

    option("cooldown_ticks", 600)
    option("radius", 16.0)

    listener<EntityDamageByEntityEvent>(
        ignoreCancelled = true,
        playerFrom = { event ->
            when (val damager = event.damager) {
                is Player -> damager
                is Projectile -> damager.shooter as? Player
                else -> null
            }
        }
    ) { player, event, config ->
        val target = event.entity as? LivingEntity ?: return@listener
        // Don't let a magic player retarget undead to themself
        if (target is Player && target.uniqueId == player.uniqueId) return@listener

        val abilityKey = Key.key("magicorigins", "undead_commander")
        val api = OriginsApi.getOrNull() ?: return@listener
        if (api.hasCooldown(player, abilityKey)) return@listener

        val radius = config.getDouble("radius", 16.0)
        val cooldown = config.getInt("cooldown_ticks", 600)
        val commanded = AtomicBoolean(false)
        event.damager.world.getNearbyEntitiesByType(Monster::class.java, event.entity.location, radius)
            .forEach { monster ->
                if (!EntityTags.UNDEADS.isTagged(monster.type)) return@forEach
                monster.runTask(OriginsReforged.instance) {
                    if (!monster.isValid || target.uniqueId == monster.uniqueId) return@runTask
                    val currentTarget = monster.target
                    if (currentTarget != null && currentTarget.uniqueId != player.uniqueId) return@runTask
                    monster.target = target
                    if (commanded.compareAndSet(false, true)) {
                        player.runTask(OriginsReforged.instance) {
                            api.setCooldown(player, abilityKey, cooldown, "undead_commander")
                        }
                    }
                }
            }
    }
}

/**
 * Resurrection Spell - Left-click with nothing in hand to resurrect every dead player
 * whose death location is within 16 blocks. 5 minute cooldown.
 *
 * With the totem-effect option (default on), each revived player gets Regeneration,
 * Fire Resistance, and Absorption (matching the legacy behaviour).
 */
val bringBackDead = ability("bring_back_dead", "magicorigins") {
    title = text("Resurrection Spell")
    description("When you swing your fist, nearby dead players that have not yet respawned come back where they died.")

    option("cooldown_ticks", 6000)
    option("use_totem_effects", true)
    option("search_radius", 16.0)

    onLeftClick { player, item, config ->
        if (item != null) return@onLeftClick false

        val abilityKey = Key.key("magicorigins", "bring_back_dead")
        val api = OriginsApi.getOrNull() ?: return@onLeftClick false
        if (api.hasCooldown(player, abilityKey)) return@onLeftClick false

        val useTotem = config.getBoolean("use_totem_effects", true)
        val radius = config.getDouble("search_radius", 16.0)

        var revivedAny = false
        Bukkit.getOnlinePlayers().forEach { other ->
            if (!other.isDead) return@forEach
            val deathLoc: Location = other.lastDeathLocation ?: return@forEach
            val revive = deathLoc.clone().add(0.5, 0.0, 0.5)
            revive.pitch = other.location.pitch
            revive.yaw = other.location.yaw
            if (revive.world != player.world) return@forEach
            if (revive.distance(player.location) > radius) return@forEach

            val originalBed: Location? = other.respawnLocation?.clone()
            if (!other.runTask(OriginsReforged.instance) {
                    other.respawnLocation = revive
                }
            ) return@forEach
            revivedAny = true

            // Respawn next tick so Bukkit has fully processed the death
            other.runTaskLater(OriginsReforged.instance, 1L) {
                if (!other.isDead) return@runTaskLater
                other.spigot().respawn()
                other.health = 2.0
                if (useTotem) {
                    other.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 900, 1, false, true, true))
                    other.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0, false, true, true))
                    other.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, 100, 1, false, true, true))
                    other.respawnLocation = originalBed
                }
            }
            other.runTaskLater(OriginsReforged.instance, 5L) {
                if (!other.isDead) other.teleport(revive)
            }
            if (useTotem) {
                other.runTaskLater(OriginsReforged.instance, 3L) {
                    MagicEffects.playTotemEffect(other)
                }
            }
        }

        if (revivedAny) {
            val cooldown = config.getInt("cooldown_ticks", 6000)
            api.setCooldown(player, abilityKey, cooldown, "bring_back_dead")
        }
        false
    }
}

/**
 * Spirit Strength is a multi-ability in the original (MultiAbility wrapper).
 * We register it as three separate abilities keyed under magicorigins:
 *   - spirit_strength:   marker / parent for UI grouping
 *   - nether_strong:     attack damage +/- 20% conditional
 *   - nether_health:     max health +/- 20% conditional
 *
 * Only the parent key is listed in origin JSONs; the sub-abilities are
 * internal and normally hidden from the UI.
 */
val netherStrong = ability("nether_strong", "magicorigins") {
    title = text("Nether Strength")
    visible = false

    conditionalAttribute(
        type = AttributeType.ATTACK_DAMAGE,
        operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        checkInterval = 40
    ) { player, _ ->
        if (player.world.environment == World.Environment.NETHER) 0.2 else -0.2
    }
}

val netherHealth = ability("nether_health", "magicorigins") {
    title = text("Nether Health")
    visible = false

    conditionalAttribute(
        type = AttributeType.MAX_HEALTH,
        operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        checkInterval = 40
    ) { player, _ ->
        if (player.world.environment == World.Environment.NETHER) 0.2 else -0.2
    }
}

val spiritStrength = ability("spirit_strength", "magicorigins") {
    title = text("Spirit Strength")
    description("You are stronger in the Nether, but weaker outside of it.")

    // The real work is done by the two sub-abilities; this parent ability is
    // a purely cosmetic entry and performs no tick work.
    onTick(interval = 200) { _, _ -> true }
}

/**
 * Undead Ally - Undead mobs won't target the player unless the player attacked them first.
 *
 * Per-player memory of provoking attacks lives in [undeadAllyAttackMemory].
 */
internal val undeadAllyAttackMemory: ConcurrentHashMap<UUID, MutableSet<UUID>> = ConcurrentHashMap()

val undeadAlly = ability("undead_ally", "magicorigins") {
    title = text("Undead Ally")
    description("With your power over darkness, undead creatures will not attack you unprovoked.")

    onAttack { player, target, _ ->
        if (target is LivingEntity && EntityTags.UNDEADS.isTagged(target.type)) {
            undeadAllyAttackMemory
                .computeIfAbsent(player.uniqueId) { ConcurrentHashMap.newKeySet() }
                .add(target.uniqueId)
        }
    }

    onEntityTarget { player, attacker, _ ->
        if (!EntityTags.UNDEADS.isTagged(attacker.type)) return@onEntityTarget true
        val provokers = undeadAllyAttackMemory[player.uniqueId]
        provokers != null && attacker.uniqueId in provokers
    }
}

/**
 * Dark Aura - Cancels any right-click trading interaction with a villager and trails
 * soul particles behind the player.
 */
val darkAura = ability("dark_aura", "magicorigins") {
    title = text("Dark Aura")
    description("You emit a dark aura that makes villagers afraid of you.")

    particles(ParticleTypes.SOUL, frequency = 4, count = 1)

    listener<PlayerInteractEntityEvent>(
        ignoreCancelled = false,
        playerFrom = { it.player }
    ) { _, event, _ ->
        if (event.rightClicked.type == EntityType.VILLAGER) {
            event.isCancelled = true
        }
    }
}

val necromancerAbilities = listOf(
    undeadCommander,
    bringBackDead,
    spiritStrength,
    netherStrong,
    netherHealth,
    undeadAlly,
    darkAura
)
