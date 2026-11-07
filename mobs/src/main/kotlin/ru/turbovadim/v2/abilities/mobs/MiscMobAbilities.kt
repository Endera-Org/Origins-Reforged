package ru.turbovadim.v2.abilities.mobs

import ru.turbovadim.v2.util.refreshPotionEffect

import com.destroystokyo.paper.MaterialTags
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.World
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Trident
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.SlimeSplitEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import ru.turbovadim.v2.ability.AbilityConfigAccessor
import ru.turbovadim.v2.api.OriginsApi
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
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
import org.endera.enderalib.utils.async.runTask
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
        player.swingMainHand()

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
        if (world.environment != World.Environment.NORMAL) return@onTick true

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
    description("Your speed increases when 3 or more other players are around you.")

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
        if (player.isDead || player.gameMode == GameMode.SPECTATOR) return@onTick false
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
        // Entity-tied: food/saturation update must run on the player's region thread.
        player.runTask(OriginsReforged.instance) {
            player.foodLevel = min(player.foodLevel + extraFood, 20)
            player.saturation = min(player.saturation + extraSaturation, player.foodLevel.toFloat())
        }
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

private val TRIDENT_EXPERT_KEY = Key.key("moborigins", "trident_expert")

/** Trident mode is the first `active_ticks` of the cooldown started by activating it. */
private fun isTridentModeActive(player: Player, config: AbilityConfigAccessor): Boolean {
    val api = OriginsApi.getOrNull() ?: return false
    val cooldown = config.getInt("cooldown_ticks", 1200)
    return api.getCooldownTicks(player, TRIDENT_EXPERT_KEY) > cooldown - config.getInt("active_ticks", 400)
}

private fun riptideSound(level: Int) = when {
    level >= 3 -> Sound.ITEM_TRIDENT_RIPTIDE_3
    level == 2 -> Sound.ITEM_TRIDENT_RIPTIDE_2
    else -> Sound.ITEM_TRIDENT_RIPTIDE_1
}

/**
 * Trident Expert - left click with an empty hand or a trident for 20 seconds of +2 trident damage,
 * Channeling without a thunderstorm and Riptide without rain (at extra durability).
 */
val tridentExpert = ability("trident_expert", "moborigins") {
    title = text("Trident Expert")
    description(
        "Left click with an empty hand or a trident to master your trident for 20 seconds:",
        "+2 trident damage, Channeling without a thunderstorm and Riptide without rain."
    )

    option("cooldown_ticks", 1200)
    option("active_ticks", 400)
    option("damage_bonus", 2.0)
    option("riptide_durability_cost", 10)

    onPrimaryAction { player, config ->
        val held = player.inventory.itemInMainHand.type
        if (!held.isAir && held != Material.TRIDENT) return@onPrimaryAction
        val api = OriginsApi.getOrNull() ?: return@onPrimaryAction
        if (api.hasCooldown(player, TRIDENT_EXPERT_KEY)) return@onPrimaryAction

        api.setCooldown(player, TRIDENT_EXPERT_KEY, config.getInt("cooldown_ticks", 1200), "trident_expert")
        player.world.playSound(player, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1f, 1f)
    }

    modifyDamage(
        outgoing = { player, damage, _, config ->
            if (player.inventory.itemInMainHand.type == Material.TRIDENT && isTridentModeActive(player, config)) {
                DamageResult.Modify(damage + config.getDouble("damage_bonus", 2.0))
            } else {
                DamageResult.Allow
            }
        }
    )

    listener<EntityDamageByEntityEvent>(
        playerFrom = { event -> (event.damager as? Trident)?.shooter as? Player }
    ) { player, event, config ->
        if (isTridentModeActive(player, config)) event.damage += config.getDouble("damage_bonus", 2.0)
    }

    // Channeling outside a thunderstorm; vanilla already handles it during one.
    listener<ProjectileHitEvent>(
        playerFrom = { event -> (event.entity as? Trident)?.shooter as? Player }
    ) { player, event, config ->
        val trident = event.entity as? Trident ?: return@listener
        val hit = event.hitEntity ?: return@listener
        if (trident.world.isThundering || !trident.itemStack.containsEnchantment(Enchantment.CHANNELING)) return@listener
        if (!isTridentModeActive(player, config)) return@listener
        hit.world.strikeLightning(hit.location)
    }

    // Riptide outside water/rain. The client refuses to charge such a trident, so the launch is instant.
    onRightClick { player, item, _, config ->
        if (item?.type != Material.TRIDENT) return@onRightClick false
        val level = item.getEnchantmentLevel(Enchantment.RIPTIDE)
        if (level <= 0 || player.isInWaterOrRainOrBubbleColumn) return@onRightClick false
        if (player.hasCooldown(Material.TRIDENT) || !isTridentModeActive(player, config)) return@onRightClick false

        @Suppress("DEPRECATION")
        if (player.isOnGround) OriginsReforged.NMSInvoker.tridentMove(player)
        player.velocity = player.velocity.add(player.location.direction.multiply(3.0 * (1 + level) / 4.0))
        player.startRiptideAttack(20, 8f, item)
        player.setCooldown(Material.TRIDENT, 20)
        player.damageItemStack(EquipmentSlot.HAND, config.getInt("riptide_durability_cost", 10))
        player.world.playSound(player, riptideSound(level), SoundCategory.PLAYERS, 1f, 1f)
        true
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
            player.refreshPotionEffect(PotionEffect(PotionEffectType.REGENERATION, duration, 0, false, true))
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

    flight {
        speed = 0.1f
        condition { player, _ -> player.isInLava && !player.isSneaking }
    }

    onTick(interval = 1) { player, _ ->
        if (player.isInLava && !player.isSneaking && player.allowFlight &&
            player.gameMode == org.bukkit.GameMode.SURVIVAL) player.isFlying = true
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

private val SPLIT_KEY = Key.key("moborigins", "split")

/** The last living entity each slime owner hit; their slimes go after it. */
private val lastHitByOwner = ConcurrentHashMap<UUID, LivingEntity>()

private val Entity.slimeOwner: UUID?
    get() = persistentDataContainer.get(slimeOwnerKey, PersistentDataType.STRING)?.let(UUID::fromString)

private fun lastVictim(owner: UUID, slime: Entity): LivingEntity? =
    lastHitByOwner[owner]?.takeIf { it.isValid && it.world == slime.world && it.location.distanceSquared(slime.location) <= 256 }

/**
 * Split - turn food into a slime that attacks whatever its owner last hit and never its owner.
 */
val split = ability("split", "moborigins") {
    title = text("Split Ability")
    description("Turn your food points into a small slime that fights for you!")

    option("cooldown_ticks", 600)
    option("food_cost", 8)
    option("slime_size", 2)

    onPrimaryAction { player, config ->
        if (!player.inventory.itemInMainHand.type.isAir) return@onPrimaryAction
        val api = OriginsApi.getOrNull() ?: return@onPrimaryAction
        if (api.hasCooldown(player, SPLIT_KEY)) return@onPrimaryAction

        val foodCost = config.getInt("food_cost", 8)
        if (player.foodLevel < foodCost) return@onPrimaryAction
        player.foodLevel -= foodCost

        val owner = player.uniqueId
        val slime = player.world.spawn(player.location, Slime::class.java) {
            it.size = config.getInt("slime_size", 2)
            it.persistentDataContainer.set(slimeOwnerKey, PersistentDataType.STRING, owner.toString())
        }
        // Slimes only hurt players and golems by touch, so hit the target directly. Runs on the slime's region.
        slime.scheduler.runAtFixedRate(OriginsReforged.instance, { _ ->
            val target = slime.target?.takeIf { it.isValid && it.uniqueId != owner } ?: lastVictim(owner, slime)
            slime.target = target
            if (target != null && target.location.distanceSquared(slime.location) <= 2.25) slime.attack(target)
        }, null, 10L, 10L)

        api.setCooldown(player, SPLIT_KEY, config.getInt("cooldown_ticks", 600), "split")
    }

    listener<EntityDamageByEntityEvent>(
        playerFrom = { event ->
            when (val damager = event.damager) {
                is Player -> damager
                is Projectile -> damager.shooter as? Player
                else -> null
            }
        }
    ) { player, event, _ ->
        val victim = event.entity as? LivingEntity ?: return@listener
        if (victim.slimeOwner != player.uniqueId) lastHitByOwner[player.uniqueId] = victim
    }

    // Owned slimes never go after or hurt their owner.
    listener<EntityTargetLivingEntityEvent>(
        playerFrom = { event ->
            val owner = event.entity.slimeOwner ?: return@listener null
            (event.target as? Player)?.takeIf { it.uniqueId == owner }
        }
    ) { player, event, _ ->
        val victim = lastVictim(player.uniqueId, event.entity)
        if (victim != null) event.target = victim else event.isCancelled = true
    }

    listener<EntityDamageByEntityEvent>(
        playerFrom = { event ->
            val owner = event.damager.slimeOwner ?: return@listener null
            (event.entity as? Player)?.takeIf { it.uniqueId == owner }
        }
    ) { _, event, _ ->
        event.isCancelled = true
    }

    // Splitting would leave ordinary hostile slimes behind.
    listener<SlimeSplitEvent>(
        playerFrom = { event -> event.entity.slimeOwner?.let(Bukkit::getPlayer) }
    ) { _, event, _ ->
        event.isCancelled = true
    }

    listener<PlayerQuitEvent>(playerFrom = { it.player }) { player, _, _ ->
        lastHitByOwner.remove(player.uniqueId)
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
