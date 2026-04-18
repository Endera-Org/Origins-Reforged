package ru.turbovadim.v2.abilities.magic

import net.kyori.adventure.key.Key
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import ru.turbovadim.ShortcutUtils
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.api.OriginsApi
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import org.bukkit.potion.PotionEffect
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Hypnotist origin abilities ported from Origins-Magic (StarshooterCity).
 *
 * Includes:
 *   - control_monsters: Right-click a monster to redirect it toward your last attacked target.
 *   - interactions_give_nausea: Right-clicking a player gives them Nausea for 30 seconds.
 */

/**
 * Tracks the two most recently attacked living entities for each player.
 */
internal object HypnosisMemory {
    private val lastHurt: ConcurrentHashMap<UUID, UUID> = ConcurrentHashMap()
    private val secondLastHurt: ConcurrentHashMap<UUID, UUID> = ConcurrentHashMap()
    private val entityLookup: ConcurrentHashMap<UUID, LivingEntity> = ConcurrentHashMap()

    fun recordHit(player: Player, target: LivingEntity) {
        val currentLastId = lastHurt[player.uniqueId]
        if (currentLastId != null && currentLastId != target.uniqueId) {
            secondLastHurt[player.uniqueId] = currentLastId
        }
        lastHurt[player.uniqueId] = target.uniqueId
        entityLookup[target.uniqueId] = target
    }

    fun lastTarget(player: Player): LivingEntity? =
        lastHurt[player.uniqueId]?.let(::resolve)

    fun secondLastTarget(player: Player): LivingEntity? =
        secondLastHurt[player.uniqueId]?.let(::resolve)

    private fun resolve(id: UUID): LivingEntity? {
        val cached = entityLookup[id]
        if (cached != null && cached.isValid && !cached.isDead) return cached
        entityLookup.remove(id)
        return null
    }
}

/**
 * Hypnosis - Right-click a monster to make it target your last (or second-last) hit victim.
 * 30 second cooldown.
 */
val controlMonsters = ability("control_monsters", "magicorigins") {
    title = text("Hypnosis")
    description("Right clicking a monster will hypnotise it to target the last other thing you attacked.")

    option("cooldown_ticks", 600)

    // Track the last two entities the player has damaged
    listener<EntityDamageByEntityEvent>(
        ignoreCancelled = false,
        playerFrom = { event ->
            val attacker = ShortcutUtils.getLivingDamageSource(event)
            attacker as? Player
        }
    ) { player, event, _ ->
        val damaged = event.entity as? LivingEntity ?: return@listener
        HypnosisMemory.recordHit(player, damaged)
    }

    onEntityInteract { player, entity, _, config ->
        val monster = entity as? Monster ?: return@onEntityInteract false

        val abilityKey = Key.key("magicorigins", "control_monsters")
        val api = OriginsApi.getOrNull() ?: return@onEntityInteract false
        if (api.hasCooldown(player, abilityKey)) return@onEntityInteract false

        var target: LivingEntity? = HypnosisMemory.lastTarget(player)
        if (target == monster) {
            target = HypnosisMemory.secondLastTarget(player)
        }
        if (target == null || target == monster || target.isDead) return@onEntityInteract false

        val cooldown = config.getInt("cooldown_ticks", 600)
        api.setCooldown(player, abilityKey, cooldown, "control_monsters")

        monster.target = target
        player.swingMainHand()
        monster.world.spawnParticle(
            Particle.SOUL,
            monster.location.clone().add(0.0, 1.0, 0.0),
            10,
            0.25, 0.5, 0.25, 0.0
        )
        true
    }
}

/**
 * Confusion - Right-click a player to give them Nausea for 30 seconds.
 */
val interactionsGiveNausea = ability("interactions_give_nausea", "magicorigins") {
    title = text("Confusion")
    description("Right clicking on a player will give them Nausea for 30 seconds.")

    option("effect_duration", 600)
    option("cooldown_ticks", 600)

    onEntityInteract { player, entity, _, config ->
        val target = entity as? Player ?: return@onEntityInteract false

        val abilityKey = Key.key("magicorigins", "interactions_give_nausea")
        val api = OriginsApi.getOrNull() ?: return@onEntityInteract false
        if (api.hasCooldown(player, abilityKey)) return@onEntityInteract false

        val cooldown = config.getInt("cooldown_ticks", 600)
        val duration = config.getInt("effect_duration", 600)

        api.setCooldown(player, abilityKey, cooldown, "nausea")
        target.addPotionEffect(
            PotionEffect(OriginsReforged.NMSInvoker.nauseaEffect, duration, 0, false, false, true)
        )
        player.swingMainHand()
        target.world.spawnParticle(
            Particle.SOUL,
            target.location.clone().add(0.0, 1.0, 0.0),
            10,
            0.25, 0.5, 0.25, 0.0
        )
        true
    }
}

val hypnotistAbilities = listOf(
    controlMonsters,
    interactionsGiveNausea
)
