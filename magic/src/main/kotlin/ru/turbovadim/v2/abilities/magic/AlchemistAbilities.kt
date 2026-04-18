package ru.turbovadim.v2.abilities.magic

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.ThrownPotion
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.inventory.EquipmentSlot
import ru.turbovadim.v2.api.OriginsApi
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text

/**
 * Alchemist origin abilities ported from Origins-Magic (StarshooterCity).
 *
 * Includes:
 *   - potion_master: Drunk and thrown potions are boosted (strength + duration).
 *   - gold_converter: Right-clicking a Copper Block swaps it with a Gold Block
 *                     (and vice-versa) with a short cooldown.
 */

/**
 * Potion Master - Boosts the amplifier and duration of potions the player drinks
 * or splash/lingering potions they throw.
 *
 * Implemented via two listeners:
 *   - EntityPotionEffectEvent (POTION_DRINK cause) is cancelled, then the effect
 *     is re-applied with a stronger amplifier / longer duration.
 *   - ProjectileLaunchEvent mutates the thrown potion's PotionMeta so that all
 *     effects (base type's defaults + custom) get the boost too.
 */
val potionMaster = ability("potion_master", "magicorigins") {
    title = text("Potion Master")
    description("Potions you drink and throw are much stronger.")

    option("strength_increase", 1)
    option("duration_multiplier", 2.0f)

    // Boost potion effects on drink
    listener<EntityPotionEffectEvent>(
        ignoreCancelled = false,
        playerFrom = { it.entity as? Player }
    ) { player, event, config ->
        val newEffect = event.newEffect ?: return@listener
        if (event.cause != EntityPotionEffectEvent.Cause.POTION_DRINK) return@listener

        val strengthIncrease = config.getInt("strength_increase", 1)
        val durationMultiplier = config.getFloat("duration_multiplier", 2.0f)

        event.isCancelled = true
        val boosted = newEffect
            .withAmplifier(newEffect.amplifier + strengthIncrease)
            .withDuration((newEffect.duration.toFloat() * durationMultiplier).toInt())
        player.addPotionEffect(boosted)
    }

    // Boost effects on thrown potions (splash + lingering)
    listener<ProjectileLaunchEvent>(
        ignoreCancelled = false,
        playerFrom = { event ->
            val potion = event.entity as? ThrownPotion ?: return@listener null
            potion.shooter as? Player
        }
    ) { _, event, config ->
        val potion = event.entity as? ThrownPotion ?: return@listener
        val meta = potion.potionMeta

        val strengthIncrease = config.getInt("strength_increase", 1)
        val durationMultiplier = config.getFloat("duration_multiplier", 2.0f)
        val isLingering = potion.item.type == Material.LINGERING_POTION

        // Combine custom effects with base type defaults
        val combined = meta.customEffects.toMutableList()
        val basePotion = meta.basePotionType
        basePotion?.potionEffects?.forEach { effect ->
            val effectiveDuration = if (isLingering) effect.duration / 4 else effect.duration
            combined += effect.withDuration(effectiveDuration)
        }

        for (effect in combined) {
            val boosted = effect
                .withAmplifier(effect.amplifier + strengthIncrease)
                .withDuration((effect.duration.toFloat() * durationMultiplier).toInt())
            meta.addCustomEffect(boosted, true)
        }
        potion.potionMeta = meta
    }
}

/**
 * Alchemy - Right-click a Copper Block to turn it into Gold, or vice-versa.
 * Short cooldown (5 seconds / 100 ticks) is shared between both directions.
 */
val goldConverter = ability("gold_converter", "magicorigins") {
    title = text("Alchemy")
    description("Right clicking on a Copper Block will turn it to Gold, and vice versa.")

    option("cooldown_ticks", 100)

    onInteract(Action.RIGHT_CLICK_BLOCK) { player, event, config ->
        if (event.hand == null || event.hand == EquipmentSlot.OFF_HAND) return@onInteract false
        if (event.hasItem()) return@onInteract false
        val block = event.clickedBlock ?: return@onInteract false

        val isCopper = block.type == Material.COPPER_BLOCK
        val isGold = block.type == Material.GOLD_BLOCK
        if (!isCopper && !isGold) return@onInteract false

        val abilityKey = Key.key("magicorigins", "gold_converter")
        val api = OriginsApi.getOrNull() ?: return@onInteract false
        if (api.hasCooldown(player, abilityKey)) return@onInteract false

        val cooldown = config.getInt("cooldown_ticks", 100)
        api.setCooldown(player, abilityKey, cooldown, "gold_converter")

        block.type = if (isCopper) Material.GOLD_BLOCK else Material.COPPER_BLOCK
        player.swingMainHand()
        block.world.playSound(
            player as Entity,
            if (isCopper) Sound.BLOCK_RESPAWN_ANCHOR_CHARGE else Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE,
            SoundCategory.BLOCKS,
            1.0f,
            1.0f
        )
        block.world.spawnParticle(
            Particle.GLOW,
            block.location.add(0.5, 0.5, 0.5),
            30,
            0.25, 0.25, 0.25, 0.0
        )
        false
    }
}

val alchemistAbilities = listOf(
    potionMaster,
    goldConverter
)
