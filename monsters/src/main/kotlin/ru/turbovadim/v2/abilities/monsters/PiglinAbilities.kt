package ru.turbovadim.v2.abilities.monsters

import com.destroystokyo.paper.MaterialTags
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Piglin
import org.bukkit.entity.Player
import org.bukkit.event.entity.PiglinBarterEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.loot.LootContext
import org.bukkit.loot.LootTables
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.ability.DamageResult
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import java.util.Locale
import java.util.Random

/**
 * Piglin-specific abilities for monster origins.
 */

private val random = Random()

val betterGoldArmour = ability("better_gold_armour", "monsterorigins") {
    title = text("Gold Worshipper")
    description(
        "Your adoration for gold unlocks its hidden power,",
        "making golden armor unbreakable and as strong as diamond."
    )

    option("helmet_bonus", 1)
    option("chestplate_bonus", 3)
    option("leggings_bonus", 3)
    option("boots_bonus", 2)

    conditionalAttribute(
        type = AttributeType.ARMOR,
        checkInterval = 5,
        valueProvider = { player, config ->
            var amount = 0
            for (item in player.equipment.armorContents) {
                if (item == null) continue
                when (item.type) {
                    Material.GOLDEN_HELMET -> amount += config.getInt("helmet_bonus", 1)
                    Material.GOLDEN_CHESTPLATE -> amount += config.getInt("chestplate_bonus", 3)
                    Material.GOLDEN_LEGGINGS -> amount += config.getInt("leggings_bonus", 3)
                    Material.GOLDEN_BOOTS -> amount += config.getInt("boots_bonus", 2)
                    else -> {}
                }
            }
            amount.toDouble()
        }
    )

    listener<PlayerItemDamageEvent>(
        ignoreCancelled = false,
        playerFrom = { it.player }
    ) { _, event, _ ->
        if (!MaterialTags.ARMOR.isTagged(event.item)) return@listener
        if (!event.item.type.toString().lowercase(Locale.ROOT).contains("gold")) return@listener
        event.isCancelled = true
    }
}

val betterGoldWeapons = ability("better_gold_weapons", "monsterorigins") {
    title = text("Gold Desecration")
    description(
        "Your evil corruption of gold unlocks a dark power,",
        "making golden weapons unbreakable and much stronger."
    )

    option("damage_multiplier", 2.0)

    modifyDamage(
        outgoing = { player, damage, _, config ->
            val itemType = player.inventory.itemInMainHand.type
            if (itemType == Material.GOLDEN_SWORD || itemType == Material.GOLDEN_AXE) {
                val multiplier = config.getDouble("damage_multiplier", 2.0)
                DamageResult.Modify(damage * multiplier)
            } else {
                DamageResult.Allow
            }
        }
    )

    listener<PlayerItemDamageEvent>(
        ignoreCancelled = false,
        playerFrom = { it.player }
    ) { _, event, _ ->
        if (event.item.type == Material.GOLDEN_SWORD || event.item.type == Material.GOLDEN_AXE) {
            event.isCancelled = true
        }
    }
}

val superBartering = ability("super_bartering", "monsterorigins") {
    title = text("Bartering Master")
    description(
        "You're brilliant at bartering after a lifetime of experience,",
        "every time you barter you get between 2 and 5 times as many valuables."
    )

    option("min_extra_drops", 1)
    option("max_extra_drops", 4)

    listener<PiglinBarterEvent>(
        ignoreCancelled = false,
        playerFrom = { event ->
            OriginsReforged.NMSInvoker.getNearestVisiblePlayer(event.entity)
        }
    ) { player, event, config ->
        val min = config.getInt("min_extra_drops", 1)
        val max = config.getInt("max_extra_drops", 4)
        val extraRounds = random.nextInt(min, max + 1)
        repeat(extraRounds) {
            val items = getBarterResponseItems(event.entity)
            throwItemsTowardPlayer(event.entity, player, items)
        }
    }
}

private fun getBarterResponseItems(piglin: Piglin): MutableCollection<ItemStack> {
    return LootTables.PIGLIN_BARTERING.lootTable
        .populateLoot(random, LootContext.Builder(piglin.location).lootedEntity(piglin).build())
}

private fun throwItemsTowardPlayer(piglin: Piglin, player: Player, items: Collection<ItemStack>) {
    throwItemsTowardPos(piglin, items, player.location)
}

private fun throwItemsTowardPos(piglin: Piglin, items: Collection<ItemStack>, pos: Location) {
    for (itemStack in items) {
        OriginsReforged.NMSInvoker.throwItem(piglin, itemStack, pos.clone().add(0.0, 1.0, 0.0))
    }
}
