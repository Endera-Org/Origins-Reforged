package ru.turbovadim.v2.abilities.mobs

import com.destroystokyo.paper.MaterialTags
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.BlockFace
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.Slime
import org.bukkit.event.entity.EntityDamageByBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityExhaustionEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.ability.DamageResult
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import kotlin.math.max
import kotlin.math.min

/**
 * Miscellaneous mob abilities.
 */

private val slimeOwnerKey: NamespacedKey by lazy {
    NamespacedKey(OriginsReforged.instance, "slime-target")
}

/**
 * Warped Fungus Eater - eat warped fungus for food and speed.
 */
val warpedFungusEater = ability("warped_fungus_eater", "moborigins") {
    title = text("Fungus Hunger")
    description("You can eat warped fungus to recover some hunger, along with a small speed boost.")

    option("food_restore", 1)
    option("saturation_restore", 1.0f)
    option("speed_duration", 200)

    onRightClick { player, item, _, config ->
        if (item?.type != Material.WARPED_FUNGUS) return@onRightClick false

        item.amount--
        if (player.inventory.itemInMainHand === item) {
            player.swingMainHand()
        } else {
            player.swingOffHand()
        }

        val speedDuration = config.getInt("speed_duration", 200)
        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, speedDuration, 0))

        val foodRestore = config.getInt("food_restore", 1)
        val saturationRestore = config.getFloat("saturation_restore", 1.0f)
        player.foodLevel = min(player.foodLevel + foodRestore, 20)
        player.saturation = min(player.saturation + saturationRestore, player.foodLevel.toFloat())

        true
    }
}

/**
 * Undead - burn in sunlight and take more damage from smite.
 */
val undead = ability("undead", "moborigins") {
    title = text("Undead")
    description("You are undead, and burn in the daylight. You also take more damage from smite.")

    option("smite_multiplier", 2.5)

    onTick(interval = 20) { player, _ ->
        val location = player.location
        val world = player.world

        var block = world.getHighestBlockAt(location)
        while ((MaterialTags.GLASS.isTagged(block) || MaterialTags.GLASS_PANES.isTagged(block)) && block.y >= location.y) {
            block = block.getRelative(BlockFace.DOWN)
        }
        val isExposed = block.y < location.y
        val isDay = world.isDayTime
        val isProtected = player.isInWaterOrRainOrBubbleColumn

        if (isExposed && isDay && !isProtected) {
            player.fireTicks = max(player.fireTicks, 60)
        }
        true
    }

    modifyDamage(
        incomingFromEntity = { _, attacker, damage, _, config ->
            val equipment = attacker.equipment ?: return@modifyDamage DamageResult.Allow
            val weapon = equipment.itemInMainHand
            val smiteLevel = weapon.getEnchantmentLevel(OriginsReforged.NMSInvoker.getSmiteEnchantment())
            if (smiteLevel <= 0) return@modifyDamage DamageResult.Allow
            val multiplier = config.getDouble("smite_multiplier", 2.5)
            DamageResult.Modify(damage + multiplier * smiteLevel)
        }
    )
}

val sly = ability("sly", "moborigins") {
    title = text("Sly")
    description("You are quicker.")

    attribute(AttributeType.MOVEMENT_SPEED, 0.06, configKey = "speed_bonus")
}

val timidCreature = ability("timid_creature", "moborigins") {
    title = text("Timid Creature")
    description("Your speed increases when you are around more than 3 other players.")

    option("required_players", 3)
    option("search_radius", 8.0)

    conditionalAttributeWhen(
        type = AttributeType.MOVEMENT_SPEED,
        value = 0.1,
        operation = AttributeModifier.Operation.ADD_NUMBER,
        checkInterval = 20,
        condition = { player, config ->
            val required = config.getInt("required_players", 3)
            val radius = config.getDouble("search_radius", 8.0)
            player.getNearbyEntities(radius, radius, radius)
                .count { it.type == EntityType.PLAYER } >= required
        }
    )
}

/**
 * Rideable Creature - other players can ride you.
 */
val rideableCreature = ability("rideable_creature", "moborigins") {
    title = text("Rideable Creature")
    description("Other players can ride you!")

    listener<PlayerInteractEntityEvent>(
        playerFrom = { it.rightClicked as? Player }
    ) { _, event, _ ->
        val rider = event.player
        val mount = event.rightClicked as? Player ?: return@listener
        if (rider === mount) return@listener
        rider.swingMainHand()
        mount.addPassenger(rider)
    }
}

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
                val remaining = player.inventory.addItem(item.itemStack)
                if (remaining.isEmpty()) {
                    player.playPickupItemAnimation(item)
                    item.remove()
                } else {
                    remaining.values.forEach { remainingStack ->
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
 */
val carefulGatherer = ability("careful_gatherer", "moborigins") {
    title = text("Careful Gatherer")
    description("Sweet Berry Bushes don't hurt you at all.")

    listener<EntityDamageByBlockEvent>(
        ignoreCancelled = false,
        playerFrom = { it.entity as? Player }
    ) { _, event, _ ->
        if (event.damager?.type == Material.SWEET_BERRY_BUSH) {
            event.isCancelled = true
        }
    }
}

/**
 * Better Berries - berries restore more hunger.
 */
val betterBerries = ability("better_berries", "moborigins") {
    title = text("Better Berries")
    description("Berries taste extra delicious to you!")

    option("extra_food", 2)
    option("extra_saturation", 1.0f)

    listener<PlayerItemConsumeEvent>(
        playerFrom = { it.player }
    ) { player, event, config ->
        if (event.item.type != Material.SWEET_BERRIES) return@listener
        val extraFood = config.getInt("extra_food", 2)
        val extraSaturation = config.getFloat("extra_saturation", 1.0f)
        Bukkit.getScheduler().runTask(OriginsReforged.instance, Runnable {
            player.foodLevel = min(player.foodLevel + extraFood, 20)
            player.saturation = min(player.saturation + extraSaturation, player.foodLevel.toFloat())
        })
    }
}

/**
 * Zombie Hunger - exhaust faster than normal.
 */
val zombieHunger = ability("zombie_hunger", "moborigins") {
    title = text("Zombie Hunger")
    description("Your constant hunger for flesh makes you exhaust quicker than a human.")

    option("exhaustion_multiplier", 1.5f)

    listener<EntityExhaustionEvent>(
        playerFrom = { it.entity as? Player }
    ) { _, event, config ->
        val multiplier = config.getFloat("exhaustion_multiplier", 1.5f)
        event.exhaustion *= multiplier
    }
}

/**
 * Trident Expert - +2 damage when throwing and wielding tridents.
 * Simplified: flat damage bonus for trident melee and trident projectile.
 */
val tridentExpert = ability("trident_expert", "moborigins") {
    title = text("Trident Expert")
    description("You're a master of the trident, dealing +2 damage when you throw it, and +2 melee damage with it.")

    option("damage_bonus", 2.0)

    modifyDamage(
        outgoing = { player, damage, _, config ->
            val item = player.inventory.itemInMainHand
            if (item.type == Material.TRIDENT) {
                val bonus = config.getDouble("damage_bonus", 2.0)
                DamageResult.Modify(damage + bonus)
            } else {
                DamageResult.Allow
            }
        }
    )

    // Thrown tridents: +2 damage via EntityDamageByEntityEvent where damager is a Trident owned by this player
    listener<EntityDamageByEntityEvent>(
        playerFrom = { event ->
            val projectile = event.damager as? Projectile ?: return@listener null
            if (projectile.type != EntityType.TRIDENT) return@listener null
            projectile.shooter as? Player
        }
    ) { _, event, config ->
        val bonus = config.getDouble("damage_bonus", 2.0)
        event.damage += bonus
    }
}

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

        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
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
 */
val bouncy = ability("bouncy", "moborigins") {
    title = text("Bouncy")
    description("All blocks act like slime blocks.")

    listener<PlayerMoveEvent>(
        ignoreCancelled = false,
        playerFrom = { it.player }
    ) { player, event, _ ->
        if (player.isSneaking) return@listener
        val from = event.from
        val to = event.to
        if (from.y > to.y && player.velocity.y < -0.1) {
            @Suppress("DEPRECATION")
            if (player.isOnGround) {
                OriginsReforged.NMSInvoker.bounce(player)
            }
        }
    }
}

/**
 * Lava Walk - walk on lava.
 */
val lavaWalk = ability("lava_walk", "moborigins") {
    title = text("Lava Walker")
    description("You have the ability to walk on lava source blocks!")

    option("flight_speed", 0.1f)

    onTick(interval = 1) { player, config ->
        val inLava = player.isInLava
        if (inLava && !player.isSneaking) {
            player.allowFlight = true
            player.isFlying = true
            player.flySpeed = config.getFloat("flight_speed", 0.1f)
        } else if (!inLava && player.isFlying && player.gameMode.name == "SURVIVAL") {
            player.isFlying = false
        }
        true
    }

    modifyBreakSpeed { player, baseSpeed, context, _ ->
        if (player.isInLava) {
            // Cancel underwater penalty and grounded penalty by multiplying baseSpeed
            val underwaterFactor = if (context.isUnderwater) 5f else 1f
            val groundedFactor = if (!context.isOnGround) 5f else 1f
            baseSpeed * underwaterFactor * groundedFactor
        } else {
            baseSpeed
        }
    }
}

/**
 * Split - spawn a slime from food.
 */
val split = ability("split", "moborigins") {
    title = text("Split Ability")
    description("Turn your food points into a small slime to defend you!")

    option("cooldown_ticks", 600)
    option("food_cost", 8)
    option("slime_size", 2)

    onPrimaryAction { player, config ->
        if (!player.inventory.itemInMainHand.type.isAir) return@onPrimaryAction

        val api = ru.turbovadim.v2.api.OriginsApi.getOrNull()
        val abilityKey = net.kyori.adventure.key.Key.key("moborigins", "split")
        if (api != null && api.hasCooldown(player, abilityKey)) return@onPrimaryAction

        val foodCost = config.getInt("food_cost", 8)
        if (player.foodLevel < foodCost) return@onPrimaryAction

        player.foodLevel -= foodCost
        val slimeSize = config.getInt("slime_size", 2)

        val slime = player.world.spawnEntity(player.location, EntityType.SLIME) as Slime
        slime.size = slimeSize
        slime.persistentDataContainer.set(slimeOwnerKey, PersistentDataType.STRING, player.uniqueId.toString())

        api?.setCooldown(player, abilityKey, config.getInt("cooldown_ticks", 600), "slime_ball")
    }

    // Prevent owned slimes from damaging their owner
    listener<EntityDamageByEntityEvent>(
        playerFrom = { event ->
            val slime = event.damager as? Slime ?: return@listener null
            val victim = event.entity as? Player ?: return@listener null
            val stored = slime.persistentDataContainer.get(slimeOwnerKey, PersistentDataType.STRING) ?: return@listener null
            if (stored == victim.uniqueId.toString()) victim else null
        }
    ) { _, event, _ ->
        event.isCancelled = true
    }
}

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
