package ru.turbovadim.v2.abilities.monsters

import com.destroystokyo.paper.MaterialTags
import org.bukkit.Material
import org.bukkit.entity.Piglin
import org.bukkit.entity.Player
import org.bukkit.loot.LootContext
import org.bukkit.loot.LootTables
import ru.turbovadim.v2.ability.DamageResult
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text
import java.util.Locale
import java.util.Random

/**
 * Piglin-specific abilities for monster origins.
 * Includes gold-related abilities and bartering.
 */

// ============================================
// GOLD ARMOR
// ============================================

/**
 * Better gold armour - golden armor is stronger and unbreakable.
 * Adds bonus armor points based on equipped gold armor:
 * - Helmet: +1
 * - Chestplate: +3
 * - Leggings: +3
 * - Boots: +2
 * Also prevents durability damage to gold armor.
 */
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

    // Note: Full implementation requires:
    // 1. Dynamic AttributeModifier that recalculates based on equipped armor
    // 2. PlayerItemDamageEvent handling to cancel durability loss on gold armor
    //
    // Legacy behavior for armor bonus calculation:
    // var amount = 0
    // for (item in player.equipment.armorContents) {
    //     if (item == null) continue
    //     when (item.type) {
    //         Material.GOLDEN_HELMET -> amount += 1
    //         Material.GOLDEN_CHESTPLATE -> amount += 3
    //         Material.GOLDEN_LEGGINGS -> amount += 3
    //         Material.GOLDEN_BOOTS -> amount += 2
    //     }
    // }
    // return amount.toDouble()
    //
    // Attribute: ARMOR, operation: ADD_NUMBER
    //
    // Durability prevention:
    // if (MaterialTags.ARMOR.isTagged(event.item) &&
    //     event.item.type.toString().lowercase().contains("gold")) {
    //     event.isCancelled = true
    // }

    onTick(interval = 5) { player, config ->
        // Calculate armor bonus based on equipped gold pieces
        var armorBonus = 0
        for (item in player.equipment.armorContents) {
            if (item == null) continue
            when (item.type) {
                Material.GOLDEN_HELMET -> armorBonus += config.getInt("helmet_bonus", 1)
                Material.GOLDEN_CHESTPLATE -> armorBonus += config.getInt("chestplate_bonus", 3)
                Material.GOLDEN_LEGGINGS -> armorBonus += config.getInt("leggings_bonus", 3)
                Material.GOLDEN_BOOTS -> armorBonus += config.getInt("boots_bonus", 2)
                else -> {}
            }
        }
        // Note: This would need to update an attribute modifier dynamically
        // Attribute: ARMOR, amount: armorBonus, operation: ADD_NUMBER
        true
    }
}

// ============================================
// GOLD WEAPONS
// ============================================

/**
 * Better gold weapons - golden weapons are stronger and unbreakable.
 * Doubles damage when attacking with golden sword or axe.
 * Also prevents durability damage to gold weapons.
 */
val betterGoldWeapons = ability("better_gold_weapons", "monsterorigins") {
    title = text("Gold Desecration")
    description(
        "Your evil corruption of gold unlocks a dark power,",
        "making golden weapons unbreakable and much stronger."
    )

    option("damage_multiplier", 2.0)

    // Damage multiplier when using gold sword or axe
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

    // Note: Durability prevention requires PlayerItemDamageEvent handling
    // Legacy behavior:
    // if (event.item.type == Material.GOLDEN_SWORD || event.item.type == Material.GOLDEN_AXE) {
    //     event.isCancelled = true
    // }
}

// ============================================
// BARTERING
// ============================================

/**
 * Super bartering - get more items from bartering with piglins.
 * When bartering, receive 2-5 times the normal loot.
 */
val superBartering = ability("super_bartering", "monsterorigins") {
    title = text("Bartering Master")
    description(
        "You're brilliant at bartering after a lifetime of experience,",
        "every time you barter you get between 2 and 5 times as many valuables."
    )

    option("min_extra_drops", 1)
    option("max_extra_drops", 4)

    // Note: Full implementation requires PiglinBarterEvent handling
    // Legacy behavior:
    // 1. Find nearest visible player to the piglin
    // 2. If player has ability, generate 1-4 extra loot drops
    // 3. Throw items toward the player
    //
    // val num = random.nextInt(1, 4)
    // for (i in 0 until num) {
    //     val items = LootTables.PIGLIN_BARTERING.lootTable
    //         .populateLoot(random, LootContext.Builder(piglin.location).lootedEntity(piglin).build())
    //     throwItemsTowardPlayer(piglin, player, items)
    // }
    //
    // The throwItemsTowardPlayer uses NMSInvoker.throwItem(piglin, itemStack, pos)
}
