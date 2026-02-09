package ru.turbovadim.v2.abilities.fantasy

import com.destroystokyo.paper.MaterialTags
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.DragonFireball
import org.bukkit.inventory.ItemStack
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Special and unique abilities for the Fantasy Origins module.
 * These abilities have complex or unique behaviors that don't fit other categories.
 */

/**
 * Dragon's Fireball - Launch dragon fireballs with a sword.
 * Legacy: DragonFireball
 *
 * Implementation: Right-click while holding a sword (and not clicking a block)
 * to launch a dragon fireball. Applies a 30-second cooldown to all swords.
 */
val dragonFireball = ability("dragon_fireball", "fantasyorigins") {
    title = text("Dragon's Fireball")
    description(
        "You can right click whilst holding a sword to launch a dragon's fireball,",
        "with a cooldown of 30 seconds."
    )

    option("cooldown_ticks", 600)
    option("velocity_multiplier", 1.2)

    onRightClick { player, item, block, config ->
        // Only activate when not clicking a block (air click)
        if (block != null) return@onRightClick false
        if (item == null) return@onRightClick false
        if (!MaterialTags.SWORDS.isTagged(item.type)) return@onRightClick false
        if (player.getCooldown(item.type) > 0) return@onRightClick false

        val cooldown = config.getInt("cooldown_ticks", 600)
        val velocityMultiplier = config.getDouble("velocity_multiplier", 1.2)

        // Apply cooldown to all swords
        MaterialTags.SWORDS.values.forEach { sword ->
            player.setCooldown(sword, cooldown)
        }

        // Launch the dragon fireball
        val fireball = player.launchProjectile(DragonFireball::class.java)

        // Set velocity after a tick to ensure proper direction
        // Note: In full implementation, this should use scheduler
        fireball.velocity = player.location.direction.multiply(velocityMultiplier)

        true // Event was handled
    }
}

/**
 * Half Horse - Permanent horse mount (centaur form).
 * Legacy: PermanentHorse
 *
 * Implementation: This is a complex ability that spawns and manages a horse entity
 * that the player permanently rides. It includes:
 * - Spawning a horse when the player joins/respawns
 * - Preventing dismount (redirects to rider's vehicle)
 * - Teleporting horse with player
 * - Applying speed/jump bonuses based on other abilities
 * - Handling damage redirection from horse to player
 *
 * Note: Full implementation requires extensive event handling beyond the basic DSL.
 * This ability is primarily a marker, with the complex behavior handled by
 * a dedicated PermanentHorse system/listener.
 */
val permanentHorse = ability("permanent_horse", "fantasyorigins") {
    title = text("Half Horse")
    description("You are half horse, half human.")

    // This is a marker ability. The actual horse management is handled by:
    // - ServerTickEndEvent listener for spawning horses
    // - FantasyEntityDismountEvent for preventing dismount
    // - FantasyEntityMountEvent for preventing mounting other things
    // - PlayerTeleportEvent for teleporting horse with player
    // - EntityDamageEvent for damage redirection
    // - PlayerDeathEvent/PlayerSwapOriginEvent for cleanup

    // The tick handler here just ensures the ability is active
    // and could be used for any periodic checks needed
    onTick(interval = 20) { player, config ->
        // Check if player should have a horse
        // Full implementation handled by external listener
        true
    }
}

/**
 * Careful Miner - Increased fortune when mining.
 * Legacy: FortuneIncreaser
 *
 * Implementation: When mining blocks, the drops are calculated as if the player
 * had +2 levels of Fortune enchantment on their tool.
 *
 * Note: Full implementation requires block break/drop event handling to recalculate
 * drops with the bonus fortune. The DSL provides a block break handler.
 */
val fortuneIncreaser = ability("fortune_increaser", "fantasyorigins") {
    title = text("Careful Miner")
    description(
        "Your care and mastery in the art of extracting minerals",
        "results in a much higher yield from ores than other creatures."
    )

    option("fortune_bonus", 2)

    onBlockBreak { player, block, drops, config ->
        val fortuneBonus = config.getInt("fortune_bonus", 2)

        // Get the player's tool
        val tool = player.inventory.itemInMainHand.clone()
        if (tool.type == Material.AIR) return@onBlockBreak

        // Calculate drops with bonus fortune
        // Note: Getting the fortune enchantment requires version-specific code
        // In legacy, this used NMSInvoker.getFortuneEnchantment()

        // For now, we try to use the standard enchantment
        val fortuneEnchant = try {
            Enchantment.LOOT_BONUS_BLOCKS
        } catch (e: Exception) {
            return@onBlockBreak
        }

        val currentFortune = tool.getEnchantmentLevel(fortuneEnchant)
        tool.addUnsafeEnchantment(fortuneEnchant, currentFortune + fortuneBonus)

        // Get the new drops with bonus fortune
        val bonusDrops = block.getDrops(tool, player)

        // Clear original drops and add the bonus drops
        drops.clear()
        drops.addAll(bonusDrops)
    }
}

/**
 * Dragon's Breath - Store dragon breath in bottles.
 * Legacy: BreathStorer
 *
 * Implementation: Right-click with an empty glass bottle to convert it
 * into a dragon's breath item. Simple item conversion.
 */
val breathStorer = ability("breath_storer", "fantasyorigins") {
    title = text("Dragon's Breath")
    description("By right clicking using an empty bottle, you can store your own Dragon's Breath.")

    onRightClick { player, item, block, config ->
        if (item?.type != Material.GLASS_BOTTLE) return@onRightClick false

        // Consume the glass bottle
        item.amount--

        // Give the player dragon's breath
        val dragonBreath = ItemStack(Material.DRAGON_BREATH)
        val leftover = player.inventory.addItem(dragonBreath)

        // Drop any items that don't fit in inventory
        leftover.values.forEach { overflow ->
            player.world.dropItemNaturally(player.location, overflow)
        }

        true // Event was handled
    }
}

/**
 * Collection of all special abilities.
 */
val specialAbilities = listOf(
    dragonFireball,
    permanentHorse,
    fortuneIncreaser,
    breathStorer
)
