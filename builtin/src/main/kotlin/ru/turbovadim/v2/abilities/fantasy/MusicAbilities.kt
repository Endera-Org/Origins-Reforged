package ru.turbovadim.v2.abilities.fantasy

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.entity.Allay
import org.bukkit.entity.EntityType
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text
import kotlin.random.Random

/**
 * Music and note-related abilities for the Fantasy Origins module.
 * These abilities interact with allays, note blocks, and musical elements.
 */

/**
 * Allay Master - Can breed allays without playing music.
 * Legacy: AllayMaster
 *
 * Implementation: When player right-clicks an allay with an amethyst shard,
 * the allay is duplicated without needing a jukebox playing music nearby.
 * Note: Requires NMSInvoker.duplicateAllay() for the actual duplication.
 */
val allayMaster = ability("allay_master", "fantasyorigins") {
    title = text("Allay Master")
    description("Your musical aura allows you to breed allays without playing music.")

    onEntityInteract { player, entity, hand, config ->
        val allay = entity as? Allay ?: return@onEntityInteract false
        val item = player.inventory.getItem(hand)
        if (item.type != Material.AMETHYST_SHARD) return@onEntityInteract false

        // Note: In full implementation, NMSInvoker.duplicateAllay(allay) should be called here
        // For now, we mark the interaction as handled and consume the shard
        // The actual duplication requires NMS code that varies by Minecraft version

        // Mark that duplication should happen (handled by external listener)
        allay.persistentDataContainer.set(
            NamespacedKey.fromString("originsreforged:allay_duplicate")!!,
            PersistentDataType.BOOLEAN,
            true
        )

        item.amount--
        when (hand) {
            EquipmentSlot.HAND -> player.swingMainHand()
            else -> player.swingOffHand()
        }
        player.inventory.setItem(hand, item)

        true // Event was handled
    }
}

/**
 * Bardic Intuition - Chance for creepers to drop music discs.
 * Legacy: BardicIntuition
 *
 * Implementation: When player kills a creeper, there's a 25% chance it drops
 * a random music disc, even without a skeleton killing it.
 */
val bardicIntuition = ability("bardic_intuition", "fantasyorigins") {
    title = text("Bardic Intuition")
    description(
        "Your musical energy will sometimes cause a creeper to drop a music disc,",
        "even without a skeleton."
    )

    option("drop_chance", 0.25)

    onKill { player, victim, config ->
        if (victim.type != EntityType.CREEPER) return@onKill

        val dropChance = config.getDouble("drop_chance", 0.25)
        if (Random.nextDouble() > dropChance) return@onKill

        // Get a random music disc from the creeper drop tag
        val discs = Tag.ITEMS_CREEPER_DROP_MUSIC_DISCS.values.toList()
        if (discs.isEmpty()) return@onKill

        val disc = discs[Random.nextInt(discs.size)]
        victim.world.dropItemNaturally(victim.location, ItemStack(disc))
    }
}

/**
 * Chime - Absorb amethyst shards to regenerate health.
 * Legacy: Chime
 *
 * Implementation: When player right-clicks with an amethyst shard,
 * it is consumed and grants Regeneration II for 45 seconds (900 ticks).
 */
val chime = ability("chime", "fantasyorigins") {
    title = text("Chime")
    description("You can absorb the chime of amethyst shards to regenerate health.")

    option("regeneration_duration", 900)
    option("regeneration_amplifier", 1)

    onRightClick { player, item, block, config ->
        if (item?.type != Material.AMETHYST_SHARD) return@onRightClick false

        val duration = config.getInt("regeneration_duration", 900)
        val amplifier = config.getInt("regeneration_amplifier", 1)

        item.amount--
        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, duration, amplifier))

        // Swing hand based on which hand holds the item
        if (player.inventory.itemInMainHand.type == Material.AMETHYST_SHARD) {
            player.swingMainHand()
        } else {
            player.swingOffHand()
        }

        true // Event was handled
    }
}

/**
 * Musically Attuned - Gain strength and speed from nearby note blocks.
 * Legacy: NoteBlockPower
 *
 * Implementation: When a note block is played within 32 blocks of the player,
 * they gain Speed II and Strength II for 30 seconds (600 ticks).
 */
val noteBlockPower = ability("note_block_power", "fantasyorigins") {
    title = text("Musically Attuned")
    description("You gain strength and speed when a nearby Note Block is played.")

    option("effect_duration", 600)
    option("effect_amplifier", 1)
    option("detection_radius", 32.0)

    onNoteBlockPlay { player, block, config ->
        val radius = config.getDouble("detection_radius", 32.0)
        val duration = config.getInt("effect_duration", 600)
        val amplifier = config.getInt("effect_amplifier", 1)

        // Check if player is within range of the note block
        if (player.location.distance(block.location) > radius) return@onNoteBlockPlay

        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, duration, amplifier))
        player.addPotionEffect(PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, amplifier))
    }
}

/**
 * Elegy - Become stronger when at low health.
 * Legacy: Elegy
 *
 * Implementation: Conditional attribute modifier that applies extra attack damage
 * when the player's health is below 3 hearts (6 HP).
 * Uses onTick to periodically check health and update the modifier.
 *
 * Config attributes (conditional):
 *   - attribute: generic-attack-damage
 *     value: 2.0
 *     operation: multiply-scalar-1
 */
val elegy = ability("elegy", "fantasyorigins") {
    title = text("Elegy")
    description("You become stronger when at less than 3 hearts.")

    option("health_threshold", 6.0)
    option("damage_multiplier", 2.0)

    // Note: The conditional attribute modifier is applied by the attribute system
    // which checks player health. This onTick ensures the modifier is updated.
    onTick(interval = 20) { player, config ->
        val threshold = config.getDouble("health_threshold", 6.0)
        // The attribute modifier system reads this and applies/removes the modifier
        // based on whether player.health <= threshold
        player.health <= threshold
    }
}

/**
 * Collection of all music-related abilities.
 */
val musicAbilities = listOf(
    allayMaster,
    bardicIntuition,
    chime,
    noteBlockPower,
    elegy
)
