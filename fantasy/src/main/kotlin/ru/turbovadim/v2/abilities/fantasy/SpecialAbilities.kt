package ru.turbovadim.v2.abilities.fantasy

import com.destroystokyo.paper.MaterialTags
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.DragonFireball
import org.bukkit.inventory.ItemStack
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Special and unique abilities for the Fantasy Origins module.
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
        if (block != null) return@onRightClick false
        if (item == null) return@onRightClick false
        if (!MaterialTags.SWORDS.isTagged(item.type)) return@onRightClick false
        if (player.getCooldown(item.type) > 0) return@onRightClick false

        val cooldown = config.getInt("cooldown_ticks", 600)
        val velocityMultiplier = config.getDouble("velocity_multiplier", 1.2)

        MaterialTags.SWORDS.values.forEach { sword ->
            player.setCooldown(sword, cooldown)
        }

        val fireball = player.launchProjectile(DragonFireball::class.java)

        // Re-apply velocity next tick so the vanilla spawn direction does not overwrite ours.
        Bukkit.getRegionScheduler().run(
            OriginsReforged.instance,
            fireball.location
        ) { _: ScheduledTask ->
            if (!fireball.isDead) {
                fireball.velocity = player.location.direction.multiply(velocityMultiplier)
            }
        }

        true
    }
}

/**
 * Half Horse - Marker ability. Horse spawning/mounting logic is handled by a
 * dedicated PermanentHorse listener system (out of DSL scope). The tick here
 * keeps the ability registered and visible.
 */
val permanentHorse = ability("permanent_horse", "fantasyorigins") {
    title = text("Half Horse")
    description("You are half horse, half human.")

    onTick(interval = 20) { _, _ -> true }
}

val fortuneIncreaser = ability("fortune_increaser", "fantasyorigins") {
    title = text("Careful Miner")
    description(
        "Your care and mastery in the art of extracting minerals",
        "results in a much higher yield from ores than other creatures."
    )

    option("fortune_bonus", 2)

    onBlockBreak { player, block, drops, config ->
        val fortuneBonus = config.getInt("fortune_bonus", 2)

        val tool = player.inventory.itemInMainHand.clone()
        if (tool.type == Material.AIR) return@onBlockBreak

        val fortuneEnchant = OriginsReforged.NMSInvoker.getFortuneEnchantment()
        val currentFortune = tool.getEnchantmentLevel(fortuneEnchant)
        tool.addUnsafeEnchantment(fortuneEnchant, currentFortune + fortuneBonus)

        val bonusDrops = block.getDrops(tool, player)

        drops.clear()
        drops.addAll(bonusDrops)
    }
}

val breathStorer = ability("breath_storer", "fantasyorigins") {
    title = text("Dragon's Breath")
    description("By right clicking using an empty bottle, you can store your own Dragon's Breath.")

    onRightClick { player, item, _, _ ->
        if (item?.type != Material.GLASS_BOTTLE) return@onRightClick false

        item.amount--

        val dragonBreath = ItemStack(Material.DRAGON_BREATH)
        val leftover = player.inventory.addItem(dragonBreath)

        leftover.values.forEach { overflow ->
            player.world.dropItemNaturally(player.location, overflow)
        }

        true
    }
}

val specialAbilities = listOf(
    dragonFireball,
    permanentHorse,
    fortuneIncreaser,
    breathStorer
)
