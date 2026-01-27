package ru.turbovadim.v2.abilities.mobs

import com.destroystokyo.paper.MaterialTags
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.block.BlockFace
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.entity.Slime
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text
import kotlin.math.max

/**
 * Miscellaneous mob abilities that don't fit into other categories.
 */

/**
 * Warped Fungus Eater - eat warped fungus for food and speed.
 * Legacy: On PlayerInteractEvent with right-click on WARPED_FUNGUS:
 *   - Consumes 1 fungus from hand
 *   - Adds SPEED I for 200 ticks
 *   - Restores 1 food level and 1 saturation
 *   - Has tick-based duplicate prevention
 */
val warpedFungusEater = ability("warped_fungus_eater", "moborigins") {
    title = text("Fungus Hunger")
    description("You can eat warped fungus to recover some hunger, along with a small speed boost.")

    option("food_restore", 1)
    option("saturation_restore", 1.0f)
    option("speed_duration", 200)

    // Implementation note: Requires PlayerInteractEvent handler for right-click
    // When action.isRightClick and item.type == WARPED_FUNGUS:
    //   - Swing appropriate hand
    //   - item.amount -= 1
    //   - player.addPotionEffect(SPEED, 200, 0)
    //   - player.foodLevel = min(player.foodLevel + 1, 20)
    //   - player.saturation = min(player.saturation + 1, player.foodLevel.toFloat())
}

/**
 * Undead - burn in sunlight and take more damage from smite.
 * Legacy:
 *   - Every tick: Burns player if in overworld, during day, exposed to sky, not in water/rain
 *     (skips glass/glass panes when checking sky exposure)
 *   - On EntityDamageByEntityEvent: Adds 2.5 * smite_level damage
 */
val undead = ability("undead", "moborigins") {
    title = text("Undead")
    description("You are undead, and burn in the daylight. You also take more damage from smite.")

    option("smite_multiplier", 2.5)

    onTick(interval = 20) { player, _ ->
        val location = player.location
        val world = player.world

        // Check if exposed to sky (legacy skips glass blocks when determining exposure)
        var block = world.getHighestBlockAt(location)
        while ((MaterialTags.GLASS.isTagged(block) || MaterialTags.GLASS_PANES.isTagged(block)) && block.y >= location.y) {
            block = block.getRelative(BlockFace.DOWN)
        }
        val isExposed = block.y < location.y

        // Check if in overworld (daytime check)
        val isDay = world.isDayTime
        val isProtected = player.isInWaterOrRainOrBubbleColumn

        if (isExposed && isDay && !isProtected) {
            player.fireTicks = max(player.fireTicks, 60)
        }
        true
    }

    // Note: Smite damage bonus requires EntityDamageByEntityEvent handler
    // When damaged, check if damager's active item has Smite enchantment
    // damage += 2.5 * smite_level
}

/**
 * Sly - increased movement speed.
 * Legacy: AttributeModifierAbility with MOVEMENT_SPEED +0.06 ADD_NUMBER
 */
val sly = ability("sly", "moborigins") {
    title = text("Sly")
    description("You are quicker.")

    option("speed_bonus", 0.06)
    option("attribute", "MOVEMENT_SPEED")
    option("operation", "ADD_NUMBER")
}

/**
 * Timid Creature - speed boost when near other players.
 * Legacy: Conditional AttributeModifierAbility
 *   - When >= 3 players within 8 blocks: MOVEMENT_SPEED +0.1 ADD_NUMBER
 */
val timidCreature = ability("timid_creature", "moborigins") {
    title = text("Timid Creature")
    description("Your speed increases when you are around more than 3 other players.")

    option("required_players", 3)
    option("search_radius", 8.0)
    option("speed_bonus", 0.1)
    option("attribute", "MOVEMENT_SPEED")
    option("operation", "ADD_NUMBER")

    onTick(interval = 20) { player, config ->
        val required = config.getInt("required_players", 3)
        val radius = config.getDouble("search_radius", 8.0)
        val nearbyPlayers = player.getNearbyEntities(radius, radius, radius)
            .count { it.type == EntityType.PLAYER }
        nearbyPlayers >= required
    }
}

/**
 * Rideable Creature - other players can ride you.
 * Legacy: On PlayerInteractEntityEvent when right-clicking a player with this ability:
 *   - Makes the clicking player a passenger of the clicked player
 *   - Removes passengers on origin swap
 */
val rideableCreature = ability("rideable_creature", "moborigins") {
    title = text("Rideable Creature")
    description("Other players can ride you!")

    // Implementation note: Requires PlayerInteractEntityEvent handler
    // When rightClicked is Player and clicked player has this ability:
    //   - event.player.swingMainHand()
    //   - rightClicked.addPassenger(event.player)
    // Also requires PlayerSwapOriginEvent to remove passengers
}

/**
 * Item Collector - larger item pickup radius.
 * Legacy: Every tick, picks up nearby items within 2.5 blocks
 *   - Fires EntityPickupItemEvent for compatibility
 *   - Handles partial pickups for full inventory
 */
val itemCollector = ability("item_collector", "moborigins") {
    title = text("Item Collector")
    description("You have a larger item pickup radius.")

    option("pickup_radius", 2.5)

    onTick(interval = 1) { player, config ->
        val radius = config.getDouble("pickup_radius", 2.5)
        player.getNearbyEntities(radius, radius, radius)
            .filterIsInstance<Item>()
            .filter { it.canPlayerPickup() && it.pickupDelay <= 0 }
            .forEach { item ->
                val remainingItems = player.inventory.addItem(item.itemStack)
                if (remainingItems.isEmpty()) {
                    player.playPickupItemAnimation(item)
                    item.remove()
                } else {
                    // Partial pickup - update remaining stack
                    remainingItems.values.forEach { remainingStack ->
                        val pickedUpAmount = item.itemStack.amount - remainingStack.amount
                        if (pickedUpAmount > 0) {
                            player.playPickupItemAnimation(item, pickedUpAmount)
                        }
                        item.itemStack = remainingStack
                    }
                }
            }
        true
    }
}

/**
 * Careful Gatherer - immune to sweet berry bush damage.
 * Legacy: On EntityDamageByBlockEvent when damager is SWEET_BERRY_BUSH:
 *   - Cancels the damage event
 */
val carefulGatherer = ability("careful_gatherer", "moborigins") {
    title = text("Careful Gatherer")
    description("Sweet Berry Bushes don't hurt you at all.")

    // Implementation note: Requires EntityDamageByBlockEvent handler
    // When damager?.type == SWEET_BERRY_BUSH and entity has ability:
    // event.isCancelled = true
}

/**
 * Better Berries - berries restore more hunger.
 * Legacy: On PlayerItemConsumeEvent for SWEET_BERRIES:
 *   - Adds +2 food level (capped at 20)
 *   - Adds +1 saturation (capped at food level)
 */
val betterBerries = ability("better_berries", "moborigins") {
    title = text("Better Berries")
    description("Berries taste extra delicious to you!")

    option("extra_food", 2)
    option("extra_saturation", 1.0f)

    // Implementation note: Requires PlayerItemConsumeEvent handler
    // When item.type == SWEET_BERRIES:
    //   player.foodLevel = min(player.foodLevel + extra_food, 20)
    //   player.saturation = min(player.saturation + extra_saturation, player.foodLevel.toFloat())
}

/**
 * Zombie Hunger - exhaust faster than normal.
 * Legacy: On EntityExhaustionEvent:
 *   - Multiplies exhaustion by 1.5
 */
val zombieHunger = ability("zombie_hunger", "moborigins") {
    title = text("Zombie Hunger")
    description("Your constant hunger for flesh makes you exhaust quicker than a human.")

    option("exhaustion_multiplier", 1.5f)

    // Implementation note: Requires EntityExhaustionEvent handler
    // event.exhaustion = event.exhaustion * exhaustion_multiplier
}

/**
 * Trident Expert - mastery with tridents.
 * Legacy: Complex ability with multiple handlers:
 *   - Conditional ATTACK_DAMAGE +2.0 when holding trident and within 400 ticks of activation
 *   - ProjectileLaunchEvent: +2 damage to thrown tridents
 *   - ProjectileHitEvent: Channeling without thunder
 *   - Riptide without rain/water (removes enchant temporarily, damages item more)
 * Note: This is a very complex ability requiring multiple event handlers and NMS.
 */
val tridentExpert = ability("trident_expert", "moborigins") {
    title = text("Trident Expert")
    description("You're a master of the trident, dealing +2 damage when you throw it, and +2 melee damage with it. You can also use channeling without thunder, and use riptide without rain/water at the price of extra durability.")

    option("damage_bonus", 2.0)
    option("cooldown_ticks", 400)

    // Implementation notes:
    // - Requires CooldownAbility for activation window tracking
    // - PlayerLeftClickEvent with AIR or TRIDENT in hand activates the "mode"
    // - AttributeModifierAbility: +2 ATTACK_DAMAGE when holding trident and mode active
    // - ProjectileLaunchEvent: +2 to trident.damage when mode active
    // - ProjectileHitEvent: Strike lightning if trident has CHANNELING and mode active
    // - PlayerInteractEvent: Temporarily modify riptide enchant for dry riptide
    // - PlayerStopUsingItemEvent: Apply riptide effect manually, damage item by 10
    // - Various cleanup handlers for item drops, switching slots, etc.
}

/**
 * Flower Power - regeneration when near flowers.
 * Legacy: Every 40 ticks, counts flowers in a 3-block sphere radius:
 *   - If >= 3 flowers: Apply REGENERATION I for 200 ticks
 */
val flowerPower = ability("flower_power", "moborigins") {
    title = text("Flower Power")
    description("When near multiple flowers, you gain regeneration.")

    option("required_flowers", 3)
    option("search_radius", 3.0)
    option("regen_duration", 200)

    onTick(interval = 40) { player, config ->
        val required = config.getInt("required_flowers", 3)
        val radius = config.getDouble("search_radius", 3.0).toInt()
        val duration = config.getInt("regen_duration", 200)

        val baseLoc = player.location
        var flowerCount = 0

        // Count flowers in sphere (legacy uses pre-computed sphere offsets)
        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    // Check if within sphere radius
                    if (x * x + y * y + z * z > radius * radius) continue

                    val loc = baseLoc.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                    if (Tag.FLOWERS.isTagged(loc.block.type)) {
                        flowerCount++
                    }
                }
            }
        }

        if (flowerCount >= required) {
            player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, duration, 0, false, true))
        }
        true
    }
}

/**
 * Bouncy - bounce off blocks like slime.
 * Legacy: On PlayerMoveEvent, applies bounce effect when landing (unless sneaking).
 * Note: Requires NMSInvoker.bounce for the actual bounce implementation.
 */
val bouncy = ability("bouncy", "moborigins") {
    title = text("Bouncy")
    description("All blocks act like slime blocks.")

    // Implementation note: Requires PlayerMoveEvent handler
    // When player lands on a block (unless sneaking):
    // NMSInvoker.bounce(player)
}

/**
 * Lava Walk - walk on lava.
 * Legacy: FlightAllowingAbility that:
 *   - Enables flight when in lava
 *   - Teleports player upward to "walk" on lava surface
 *   - Provides BreakSpeedModifierAbility to allow normal mining speed in lava
 *   - Has configurable flight speed
 * Note: Complex ability requiring FlightAllowingAbility and BreakSpeedModifierAbility interfaces.
 */
val lavaWalk = ability("lava_walk", "moborigins") {
    title = text("Lava Walker")
    description("You have the ability to walk on lava source blocks! You are also quicker while walking on lava, and slower on land.")

    option("flight_speed", 0.1f)

    // Implementation notes:
    // - FlightAllowingAbility.canFly: returns true when player.isInLava
    // - When in lava and not sneaking: teleport player upward to surface level
    // - BreakSpeedModifierAbility: Override underwater/onGround context when in lava
    // - FlightAllowingAbility.getFlightSpeed: returns flight_speed config value
}

/**
 * Split - spawn slimes from food.
 * Legacy: On PlayerLeftClickEvent with empty hand and food >= 8:
 *   - Consumes 8 food points
 *   - Spawns size-2 slime at player location
 *   - Tags slime with player UUID for ownership
 *   - Slime won't attack its owner and follows owner's combat targets
 */
val split = ability("split", "moborigins") {
    title = text("Split Ability")
    description("Turn your food points into a small slime to defend you!")

    option("cooldown_ticks", 600)
    option("food_cost", 8)
    option("slime_size", 2)

    onPrimaryAction { player, config ->
        if (!player.inventory.itemInMainHand.type.isAir) return@onPrimaryAction

        val foodCost = config.getInt("food_cost", 8)
        if (player.foodLevel < foodCost) return@onPrimaryAction

        player.foodLevel -= foodCost
        val slimeSize = config.getInt("slime_size", 2)

        val slime = player.world.spawnEntity(player.location, EntityType.SLIME) as Slime
        slime.size = slimeSize

        // Tag slime with owner's UUID for ownership tracking
        val slimeKey = NamespacedKey.fromString("slime-target")!!
        slime.persistentDataContainer.set(slimeKey, PersistentDataType.STRING, player.uniqueId.toString())

        // Note: Legacy has additional handlers for:
        // - EntityTargetLivingEntityEvent: Redirect slime targeting away from owner
        // - EntityDamageByEntityEvent: Cancel damage from owned slime to owner
        // - EntityMoveEvent: Make slime attack owner's combat targets
    }
}

/**
 * Collection of all miscellaneous mob abilities.
 */
val miscMobAbilities = listOf(
    warpedFungusEater,
    undead,
    sly,
    timidCreature,
    rideableCreature,
    itemCollector,
    carefulGatherer,
    betterBerries,
    zombieHunger,
    tridentExpert,
    flowerPower,
    bouncy,
    lavaWalk,
    split
)
