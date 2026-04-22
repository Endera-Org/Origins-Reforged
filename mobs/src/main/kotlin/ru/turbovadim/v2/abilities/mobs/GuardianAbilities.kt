package ru.turbovadim.v2.abilities.mobs

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.endera.enderalib.utils.async.runTask
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.api.OriginsApi
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import java.util.Random

/**
 * Guardian-related abilities for the Mobs module.
 */

private val random = Random()

/**
 * Becomes Elder Guardian - evolve into elder guardian upon defeating one.
 */
val becomesElderGuardian = ability("becomes_elder_guardian", "moborigins") {
    title = text("Become Elder Guardian")
    description("Defeating an Elder Guardian will turn you into one!")

    option("target_origin", "moborigins:elder_guardian")
    option("layer", "origin")

    listener<EntityDeathEvent>(
        playerFrom = { event ->
            if (event.entity.type != EntityType.ELDER_GUARDIAN) return@listener null
            event.entity.killer
        }
    ) { player, _, config ->
        val api = OriginsApi.getOrNull() ?: return@listener
        val targetKeyString = config.getString("target_origin", "moborigins:elder_guardian")
        val layer = config.getString("layer", "origin")

        val targetKey = runCatching { Key.key(targetKeyString) }.getOrNull() ?: return@listener
        val origin = api.getOrigin(targetKey) ?: return@listener

        api.setPlayerOrigin(player, layer, origin)
        player.sendMessage(
            Component.text("You have grown into an Elder Guardian!", NamedTextColor.YELLOW)
        )
    }
}

/**
 * Guardian Ally - guardians don't attack you.
 */
val guardianAlly = ability("guardian_ally", "moborigins") {
    title = text("Guardian Ally")
    description("Guardians don't attack you!")

    onEntityTarget { _, attacker, _ ->
        attacker.type != EntityType.GUARDIAN && attacker.type != EntityType.ELDER_GUARDIAN
    }
}

/**
 * Guardian Spikes - chance to damage attackers with thorns damage.
 */
val guardianSpikes = ability("guardian_spikes", "moborigins") {
    title = text("Guardian Spikes")
    description("Spikes that have a chance to damage attackers!")

    option("damage", 2.0)
    option("chance", 0.75)

    listener<EntityDamageByEntityEvent>(
        playerFrom = { it.entity as? Player }
    ) { player, event, config ->
        val chance = config.getDouble("chance", 0.75)
        if (random.nextDouble() > chance) return@listener

        val damager = when (val raw = event.damager) {
            is Projectile -> raw.shooter as? org.bukkit.entity.Entity ?: return@listener
            else -> raw
        }
        if (damager === player) return@listener

        val spikeDamage = config.getDouble("damage", 2.0).toInt()
        OriginsReforged.NMSInvoker.dealThornsDamage(damager, spikeDamage, player)
    }
}

/**
 * Elder Spikes - stronger spikes for elder guardians (4 thorns damage).
 */
val elderSpikes = ability("elder_spikes", "moborigins") {
    title = text("Elder Spikes")
    description("Spikes that have a chance to damage attackers!")

    option("damage", 4.0)
    option("chance", 0.75)

    listener<EntityDamageByEntityEvent>(
        playerFrom = { it.entity as? Player }
    ) { player, event, config ->
        val chance = config.getDouble("chance", 0.75)
        if (random.nextDouble() > chance) return@listener

        val damager = when (val raw = event.damager) {
            is Projectile -> raw.shooter as? org.bukkit.entity.Entity ?: return@listener
            else -> raw
        }
        if (damager === player) return@listener

        val spikeDamage = config.getDouble("damage", 4.0).toInt()
        OriginsReforged.NMSInvoker.dealThornsDamage(damager, spikeDamage, player)
    }
}

/**
 * Elder Magic - cast mining fatigue on nearby players.
 */
val elderMagic = ability("elder_magic", "moborigins") {
    title = text("Elder Magic")
    description("You can cast a spell on nearby players to slow down their mining speed.")

    option("cooldown_ticks", 600)
    option("effect_duration", 600)
    option("effect_amplifier", 1)
    option("range", 5.0)

    onPrimaryAction { player, config ->
        if (!player.inventory.itemInMainHand.type.isAir) return@onPrimaryAction

        val api = OriginsApi.getOrNull()
        val abilityKey = Key.key("moborigins", "elder_magic")
        if (api != null && api.hasCooldown(player, abilityKey)) return@onPrimaryAction

        val duration = config.getInt("effect_duration", 600)
        val amplifier = config.getInt("effect_amplifier", 1)
        val range = config.getDouble("range", 5.0)

        val miningFatigue = OriginsReforged.NMSInvoker.miningFatigueEffect
        val elderParticle = OriginsReforged.NMSInvoker.getElderGuardianParticle()

        player.getNearbyEntities(range, range, range)
            .filterIsInstance<Player>()
            .filter { it !== player && api?.hasAbility(it, abilityKey) != true }
            .forEach { target ->
                target.runTask(OriginsReforged.instance) {
                    target.addPotionEffect(PotionEffect(miningFatigue, duration, amplifier, false, true))
                    target.playSound(target, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1f, 1f)
                    target.world.spawnParticle(elderParticle, target.location, 1)
                }
            }

        api?.setCooldown(player, abilityKey, config.getInt("cooldown_ticks", 600), "prismarine_shard")
    }
}

/**
 * Mining Fatigue Immune - immune to mining fatigue effect.
 */
val miningFatigueImmune = ability("mining_fatigue_immune", "moborigins") {
    title = text("Mining Fatigue Immune")
    description("You are immune to the mining fatigue effect.")
    visible = false

    listener<EntityPotionEffectEvent>(
        playerFrom = { it.entity as? Player }
    ) { _, event, _ ->
        if (event.newEffect?.type == OriginsReforged.NMSInvoker.miningFatigueEffect) {
            event.isCancelled = true
        }
    }
}

/**
 * Prismarine Skin - natural armor from prismarine skin.
 */
val prismarineSkin = ability("prismarine_skin", "moborigins") {
    title = text("Prismarine Skin")
    description("Your skin is made of prismarine, and you get natural armor from it.")

    attribute(AttributeType.ARMOR, 2.0, configKey = "armor_bonus")
}

/**
 * Water Combatant - deal more damage while in water.
 */
val waterCombatant = ability("water_combatant", "moborigins") {
    title = text("Water Combatant")
    description("You deal more damage while in water.")

    option("damage_bonus", 3.0)

    modifyDamage(
        outgoing = { player, damage, _, config ->
            if (player.isInWater) {
                val bonus = config.getDouble("damage_bonus", 3.0)
                ru.turbovadim.v2.ability.DamageResult.Modify(damage + bonus)
            } else {
                ru.turbovadim.v2.ability.DamageResult.Allow
            }
        }
    )
}

/**
 * Surface Slowness - slower movement on land.
 */
val surfaceSlowness = ability("surface_slowness", "moborigins") {
    title = text("Surface Slowness")
    description("You move slower on land.")
    visible = false

    attribute(
        AttributeType.MOVEMENT_SPEED,
        -0.4,
        operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        configKey = "speed_reduction"
    )
}

/**
 * Surface Weakness - weakness effect while on land.
 */
val surfaceWeakness = ability("surface_weakness", "moborigins") {
    title = text("Surface Weakness")
    description("You are weakened while on land.")

    onTick(interval = 20) { player, _ ->
        if (!player.isInWater) {
            player.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 100, 0, true, true))
        } else if (player.hasPotionEffect(PotionEffectType.WEAKNESS)) {
            player.removePotionEffect(PotionEffectType.WEAKNESS)
        }
        true
    }
}

val guardianAbilities = listOf(
    becomesElderGuardian,
    guardianAlly,
    guardianSpikes,
    elderSpikes,
    elderMagic,
    miningFatigueImmune,
    prismarineSkin,
    waterCombatant,
    surfaceSlowness,
    surfaceWeakness
)
