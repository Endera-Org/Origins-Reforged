package ru.turbovadim.v2.abilities.fantasy

import io.papermc.paper.tag.EntityTags
import net.kyori.adventure.key.Key
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.ThrownPotion
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.potion.PotionEffectType
import org.endera.enderalib.utils.async.runTask
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.api.OriginsApi
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text
import ru.turbovadim.v2.dsl.listener
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * Combat-related abilities for the Fantasy Origins module.
 */

val heavyBlow = ability("heavy_blow", "fantasyorigins") {
    title = text("Heavy Blow")
    description(
        "Your attacks are stronger than humans, but you have a longer attack cooldown."
    )

    attributes {
        add(
            AttributeType.ATTACK_DAMAGE,
            1.2,
            operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
            configKey = "damage_multiplier"
        )
        add(
            AttributeType.ATTACK_SPEED,
            -0.4,
            operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
            configKey = "attack_speed_modifier"
        )
    }
}

val leeching = ability("leeching", "fantasyorigins") {
    title = text("Leeching")
    description("Upon killing a mob or player, you sap a portion of its health, healing you.")

    option("health_fraction", 0.2)

    onKill { player, victim, config ->
        val fraction = config.getDouble("health_fraction", 0.2)
        // EntityDeathEvent fires on the victim's region in Folia; read the victim's
        // max health here (we are on the victim's region) and hop to the killer's
        // region before mutating their health.
        val victimMaxHealth = victim.maxHealth
        val healAmount = victimMaxHealth * fraction

        player.runTask(OriginsReforged.instance) {
            val playerMaxHealth = player.maxHealth
            player.health = min(playerMaxHealth, player.health + healAmount)
        }
    }
}

val magicResistance = ability("magic_resistance", "fantasyorigins") {
    title = text("Iron Stomach")
    description("You have an immunity to poison and harming potion effects.")

    // Harming potions deal MAGIC damage either with no direct entity (drunk) or via the potion,
    // its lingering cloud or a tipped arrow. Guardians and evoker fangs also deal MAGIC and still hurt.
    listener<EntityDamageEvent>(playerFrom = { it.entity as? Player }) { _, event, _ ->
        if (event.cause != DamageCause.MAGIC) return@listener
        when (event.damageSource.directEntity) {
            null, is ThrownPotion, is AreaEffectCloud, is AbstractArrow -> event.isCancelled = true
        }
    }

    listener<EntityPotionEffectEvent>(
        playerFrom = { it.entity as? Player }
    ) { _, event, _ ->
        if (event.newEffect?.type == PotionEffectType.POISON) event.isCancelled = true
    }
}

/**
 * Vampiric Transformation - killing a player converts them into a vampire.
 * Uses OriginsApi to set the victim's origin to `fantasyorigins:vampire`.
 */
val vampiricTransformation = ability("vampiric_transformation", "fantasyorigins") {
    title = text("Vampiric Transformation")
    description("You can transform other players into vampires by killing them.")

    option("transform_chance", 1.0)
    option("target_origin", "fantasyorigins:vampire")
    option("layer", "origin")

    onKill { _, victim, config ->
        if (victim !is Player) return@onKill

        val chance = config.getDouble("transform_chance", 1.0)
        if (kotlin.random.Random.nextDouble() > chance) return@onKill

        val api = OriginsApi.getOrNull() ?: return@onKill
        val targetKeyString = config.getString("target_origin", "fantasyorigins:vampire")
        val layer = config.getString("layer", "origin")

        val targetKey = runCatching { Key.key(targetKeyString) }.getOrNull() ?: return@onKill
        val origin = api.getOrigin(targetKey) ?: return@onKill

        api.setPlayerOrigin(victim, layer, origin)
    }
}

/**
 * Undead mobs each player has hit (melee or projectile); only these may target them.
 */
internal val undeadAllyAttackMemory: ConcurrentHashMap<UUID, MutableSet<UUID>> = ConcurrentHashMap()

val undeadAlly = ability("undead_ally", "fantasyorigins") {
    title = text("Undead Ally")
    description("As an undead monster, other undead creatures will not attack you unprovoked.")

    listener<EntityDamageByEntityEvent>(
        playerFrom = { event ->
            when (val damager = event.damager) {
                is Player -> damager
                is Projectile -> damager.shooter as? Player
                else -> null
            }
        }
    ) { player, event, _ ->
        if (!EntityTags.UNDEADS.isTagged(event.entity.type)) return@listener
        undeadAllyAttackMemory
            .computeIfAbsent(player.uniqueId) { ConcurrentHashMap.newKeySet() }
            .add(event.entity.uniqueId)
    }

    listener<PlayerQuitEvent>(playerFrom = { it.player }) { player, _, _ ->
        undeadAllyAttackMemory.remove(player.uniqueId)
    }

    onEntityTarget { player, attacker, _ ->
        if (!EntityTags.UNDEADS.isTagged(attacker.type)) {
            return@onEntityTarget true
        }
        val provokers = undeadAllyAttackMemory[player.uniqueId]
        provokers != null && attacker.uniqueId in provokers
    }
}

val combatAbilities = listOf(
    heavyBlow,
    leeching,
    magicResistance,
    vampiricTransformation,
    undeadAlly
)
