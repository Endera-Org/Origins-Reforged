package ru.turbovadim.v2.abilities.fantasy

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Allay
import org.bukkit.entity.EntityType
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text
import kotlin.random.Random

/**
 * Music and note-related abilities for the Fantasy Origins module.
 */

val allayMaster = ability("allay_master", "fantasyorigins") {
    title = text("Allay Master")
    description("Your musical aura allows you to breed allays without playing music.")

    onEntityInteract { player, entity, hand, _ ->
        val allay = entity as? Allay ?: return@onEntityInteract false
        val item = player.inventory.getItem(hand)
        if (item.type != Material.AMETHYST_SHARD) return@onEntityInteract false

        val duplicated = OriginsReforged.NMSInvoker.duplicateAllay(allay)
        if (!duplicated) return@onEntityInteract false

        item.amount--
        when (hand) {
            EquipmentSlot.HAND -> player.swingMainHand()
            else -> player.swingOffHand()
        }
        player.inventory.setItem(hand, item)

        true
    }
}

val bardicIntuition = ability("bardic_intuition", "fantasyorigins") {
    title = text("Bardic Intuition")
    description(
        "Your musical energy will sometimes cause a creeper to drop a music disc,",
        "even without a skeleton."
    )

    option("drop_chance", 0.25)

    onKill { _, victim, config ->
        if (victim.type != EntityType.CREEPER) return@onKill

        val dropChance = config.getDouble("drop_chance", 0.25)
        if (Random.nextDouble() > dropChance) return@onKill

        val discs = Tag.ITEMS_CREEPER_DROP_MUSIC_DISCS.values.toList()
        if (discs.isEmpty()) return@onKill

        val disc = discs[Random.nextInt(discs.size)]
        victim.world.dropItemNaturally(victim.location, ItemStack(disc))
    }
}

val chime = ability("chime", "fantasyorigins") {
    title = text("Chime")
    description("You can absorb the chime of amethyst shards to regenerate health.")

    option("regeneration_duration", 900)
    option("regeneration_amplifier", 1)

    onRightClick { player, item, _, config ->
        if (item?.type != Material.AMETHYST_SHARD) return@onRightClick false

        val duration = config.getInt("regeneration_duration", 900)
        val amplifier = config.getInt("regeneration_amplifier", 1)

        item.amount--
        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, duration, amplifier))

        if (player.inventory.itemInMainHand.type == Material.AMETHYST_SHARD) {
            player.swingMainHand()
        } else {
            player.swingOffHand()
        }

        true
    }
}

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

        if (player.location.distance(block.location) > radius) return@onNoteBlockPlay

        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, duration, amplifier))
        player.addPotionEffect(PotionEffect(PotionEffectType.STRENGTH, duration, amplifier))
    }
}

val elegy = ability("elegy", "fantasyorigins") {
    title = text("Elegy")
    description("You become stronger when at less than 3 hearts.")

    option("health_threshold", 6.0)
    option("damage_multiplier", 2.0)

    conditionalAttributeWhen(
        type = AttributeType.ATTACK_DAMAGE,
        value = 2.0,
        operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        checkInterval = 20
    ) { player, config ->
        player.health <= config.getDouble("health_threshold", 6.0)
    }
}

val musicAbilities = listOf(
    allayMaster,
    bardicIntuition,
    chime,
    noteBlockPower,
    elegy
)
