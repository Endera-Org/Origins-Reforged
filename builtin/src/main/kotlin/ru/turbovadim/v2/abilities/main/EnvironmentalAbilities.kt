package ru.turbovadim.v2.abilities.main

import ru.turbovadim.v2.util.refreshPotionEffects

import com.destroystokyo.paper.MaterialTags
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Trident
import org.bukkit.event.entity.EntityAirChangeEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginsReforged
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.v2.ability.Ability
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// ============================================
// ENVIRONMENTAL ABILITIES
// ============================================

/**
 * Burn In Daylight - sets on fire in sunlight when not invisible.
 * Legacy: BurnInDaylight.kt
 *
 * The legacy implementation:
 * - Depends on phantomize (inverse - active when NOT phantomized)
 * - Checks if player is in normal world, during daytime
 * - Skips glass and glass panes when checking for cover
 * - Sets player on fire for 60 ticks minimum
 * - Runs every 15 ticks
 */
val burnInDaylight = ability("burn_in_daylight") {
    title = text("Photoallergic")
    description("You begin to burn in daylight if you are not invisible.")

    dependsOn = Key.key("origins", "phantomize")
    dependencyInverse = true

    option("check_interval", 15)
    option("fire_ticks", 60)

    onTick(interval = 15) { player, config ->
        val world = player.world
        val loc = player.location
        val playerY = loc.y

        if (world.environment != World.Environment.NORMAL) return@onTick true
        if (!world.isDayTime) return@onTick true
        if (player.isInWaterOrRainOrBubbleColumn) return@onTick true

        var block = world.getHighestBlockAt(loc)
        while ((MaterialTags.GLASS.isTagged(block) || MaterialTags.GLASS_PANES.isTagged(block)) && block.y >= playerY) {
            block = block.getRelative(BlockFace.DOWN)
        }

        if (block.y < playerY) {
            val fireTicks = config.getInt("fire_ticks", 60)
            player.fireTicks = player.fireTicks.coerceAtLeast(fireTicks)
        }
        true
    }
}

/**
 * Fresh Air - can only sleep at high altitude.
 * Legacy: FreshAir.kt
 *
 * Implementation:
 * - Cancels bed interaction if below required height
 * - Only applies in the overworld
 * - Allows sleeping if it's daytime and clear weather (just resting)
 * - Shows action bar message when prevented
 */
val freshAir: Ability = ability("fresh_air") {
    title = text("Fresh Air")
    description("When sleeping, your bed needs to be at an altitude of at least 86 blocks, so you can breathe fresh air.")

    option("required_height", 86)
    option("fail_message", "You need fresh air to sleep")

    onRightClickInteract { player, event, config ->
        val clickedBlock = event.clickedBlock ?: return@onRightClickInteract false

        val inventory = player.inventory
        if (player.isSneaking &&
            inventory.itemInOffHand.type == Material.AIR &&
            inventory.itemInMainHand.type == Material.AIR
        ) return@onRightClickInteract false

        if (!Tag.BEDS.isTagged(clickedBlock.type)) return@onRightClickInteract false

        val requiredHeight = config.getInt("required_height", 86)

        if (clickedBlock.y >= requiredHeight) return@onRightClickInteract false

        // Only applies in the overworld
        val overworldName = OriginsReforged.mainConfig.worlds.world
        val overworld = Bukkit.getWorld(overworldName) ?: return@onRightClickInteract false
        if (player.world != overworld) return@onRightClickInteract false

        // Allow sleeping during daytime with clear weather (just resting)
        val blockWorld = clickedBlock.world
        if (blockWorld.isDayTime && blockWorld.isClearWeather) return@onRightClickInteract false

        // Cancel and notify player
        player.swingMainHand()
        val message = config.getString("fail_message", "You need fresh air to sleep")
        player.sendActionBar(Component.text(message))
        true
    }
}

/**
 * Nether Spawn - spawns in the Nether by default.
 * Legacy: NetherSpawn.kt
 *
 */
val netherSpawn = ability("nether_spawn") {
    title = text("Nether Inhabitant")
    description("Your natural spawn will be in the Nether.")

    defaultSpawn { _, _ ->
        Bukkit.getWorld(OriginsReforged.mainConfig.worlds.worldNether)?.spawnLocation
    }
}

/**
 * Claustrophobia - gets weakness and slowness when under low ceilings.
 * Legacy: Claustrophobia.kt
 *
 * Implementation:
 * - Checks if block 2 above player is solid
 * - Applies weakness and slowness while the ceiling remains low
 */
val claustrophobia = ability("claustrophobia") {
    title = text("Claustrophobia")
    description("A low ceiling will weaken you and make you slower.")

    option("check_interval", 5)
    val buildup = intState("buildup", -200)

    listener<PlayerItemConsumeEvent>(playerFrom = { it.player }) { player, event, _ ->
        if (event.item.type == Material.MILK_BUCKET) buildup[player] = buildup[player].coerceAtMost(0)
    }
    onDependencyDisabled { player, _ -> buildup.reset(player) }

    onTick(interval = 5) { player, _ ->
        val blockAbove = player.location.block.getRelative(BlockFace.UP, 2)
        val duration = if (blockAbove.isSolid) (buildup[player] + 1).coerceAtMost(3600)
            else (buildup[player] - 1).coerceAtLeast(-200)
        buildup[player] = duration
        if (duration > 0) {
            player.refreshPotionEffects(
                listOf(
                    PotionEffect(PotionEffectType.WEAKNESS, duration, 0, true, true, true),
                    PotionEffect(NMSInvoker.slownessEffect, duration, 0, true, true, true)
                )
            )
        }
        true
    }
}

/** Lazily resolve the Impaling enchantment (works across versions) */
private val impalingEnchantment: Enchantment? by lazy {
    try {
        // Try registry lookup (1.20.5+)
        @Suppress("DEPRECATION")
        Registry.ENCHANTMENT.get(NamespacedKey.minecraft("impaling"))
    } catch (_: Exception) {
        try {
            // Fallback for older versions
            @Suppress("DEPRECATION")
            Enchantment.getByName("IMPALING")
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Aquatic - takes extra damage from Impaling enchantment.
 * Legacy: Aquatic.kt
 *
 * Implementation:
 * - Adds 2.5 damage per Impaling level from tridents or melee weapons
 */
val aquatic = ability("aquatic") {
    title = text("Aquatic")
    description("You are considered an aquatic creature.")
    visible = false

    option("impaling_bonus_per_level", 2.5)

    listener<EntityDamageByEntityEvent>(
        playerFrom = { it.entity as? org.bukkit.entity.Player }
    ) { _, event, config ->
        val weapon = when (val damager = event.damager) {
            is Trident -> damager.itemStack
            is LivingEntity -> damager.equipment?.itemInMainHand
            else -> null
        } ?: return@listener

        val enchantment = impalingEnchantment ?: return@listener
        val level = weapon.getEnchantmentLevel(enchantment)
        if (level <= 0) return@listener

        event.damage += config.getDouble("impaling_bonus_per_level", 2.5) * level
    }
}

/** Players whose air is currently being written by gills logic; every other air change is vetoed. */
private val gillsWritingAir = ConcurrentHashMap.newKeySet<UUID>()

/** Sets air without being vetoed by [waterBreathing]'s [EntityAirChangeEvent] listener. */
private fun Player.setAirFromGills(air: Int) {
    gillsWritingAir += uniqueId
    try {
        remainingAir = air
    } finally {
        gillsWritingAir -= uniqueId
    }
}

private fun Player.canBreatheWithGills() =
    isUnderWater || isInRain ||
        hasPotionEffect(PotionEffectType.WATER_BREATHING) ||
        hasPotionEffect(PotionEffectType.CONDUIT_POWER)

/**
 * Water Breathing - breathes underwater, drowns on land.
 * Legacy: WaterBreathing.kt
 *
 * Vanilla air changes are vetoed so air is driven only by this ability:
 * - Recovers air underwater, in rain, or with water breathing / conduit power
 * - Loses air otherwise (respiration slows the loss) and takes drowning damage once it runs out
 * - Turtle helmets grant no water breathing, since that would let you breathe on land
 */
val waterBreathing = ability("water_breathing") {
    title = text("Gills")
    description("You can breathe underwater, but not on land.")

    option("air_recovery_rate", 4)
    option("land_damage", 2)

    listener<EntityAirChangeEvent>(
        playerFrom = { it.entity as? Player }
    ) { player, event, _ ->
        if (player.uniqueId !in gillsWritingAir) event.isCancelled = true
    }

    listener<EntityPotionEffectEvent>(
        playerFrom = { it.entity as? Player }
    ) { _, event, _ ->
        if (event.cause == EntityPotionEffectEvent.Cause.TURTLE_HELMET) event.isCancelled = true
    }

    onTick(interval = 1) { player, config ->
        val recoveryRate = config.getInt("air_recovery_rate", 4)

        if (player.canBreatheWithGills()) {
            player.setAirFromGills((player.remainingAir + recoveryRate).coerceIn(0, player.maximumAir))
        } else {
            // Lose air on land
            // Check for respiration enchantment
            val respirationLevel = player.inventory.helmet
                ?.itemMeta
                ?.getEnchantLevel(NMSInvoker.respirationEnchantment) ?: 0

            // Respiration gives chance to not lose air
            val shouldLoseAir = if (respirationLevel > 0) {
                Math.random() > (respirationLevel.toDouble() / (respirationLevel + 1))
            } else {
                true
            }

            if (shouldLoseAir) {
                player.setAirFromGills(player.remainingAir - 1)
            }

            // Deal drowning damage when air runs out
            if (player.remainingAir < -20) {
                val landDamage = config.getInt("land_damage", 2)
                NMSInvoker.dealDrowningDamage(player, landDamage)
                player.setAirFromGills(0)
            }
        }
        true
    }
}

/**
 * Air From Potions - drinking potions restores air.
 * Legacy: AirFromPotions.kt
 *
 * The legacy implementation restores 60 air when consuming any potion.
 */
val airFromPotions = ability("air_from_potions") {
    title = text("Hydration")
    description("Drinking potions restores some of your air.")
    visible = false

    option("air_restored", 60)

    listener<PlayerItemConsumeEvent>(
        playerFrom = { it.player }
    ) { player, event, config ->
        if (event.item.type != Material.POTION) return@listener

        val airRestored = config.getInt("air_restored", 60)
        player.setAirFromGills((player.remainingAir + airRestored).coerceAtMost(player.maximumAir))
    }
}

/**
 * Collection of all environmental abilities.
 */
val environmentalAbilities = listOf(
    burnInDaylight,
    freshAir,
    netherSpawn,
    claustrophobia,
    aquatic,
    waterBreathing,
    airFromPotions
)
