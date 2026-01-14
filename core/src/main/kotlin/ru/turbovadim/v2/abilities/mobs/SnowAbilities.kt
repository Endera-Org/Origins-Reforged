package ru.turbovadim.v2.abilities.mobs

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text
import kotlin.math.max
import kotlin.math.min

/**
 * Snow and temperature-related abilities for the Mobs module.
 * Used by snow golem and similar cold-themed origins.
 */

// Temperature tracking state (mirrors Temperature.INSTANCE from legacy)
private val playerTemperatureMap = mutableMapOf<Player, Int>()

/**
 * Get the current temperature for a player (0-100).
 */
fun getTemperature(player: Player): Int {
    return playerTemperatureMap.getOrDefault(player, 0)
}

/**
 * Set the temperature for a player, clamped to 0-100.
 */
fun setTemperature(player: Player, amount: Int) {
    playerTemperatureMap[player] = max(0, min(amount, 100))
}

/**
 * Snow Trail - leaves a trail of snow where you walk.
 * Legacy: Every tick, places snow at player's feet if block is AIR and can place.
 */
val snowTrail = ability("snow_trail", "moborigins") {
    title = text("Snow Trail")
    description("You leave a trail of snow.")

    onTick(interval = 1) { player, _ ->
        val block = player.location.block
        if (block.type == Material.AIR) {
            val snowData = Material.SNOW.createBlockData()
            if (block.canPlace(snowData)) {
                block.type = Material.SNOW
            }
        }
        true
    }
}

/**
 * Stronger Snowballs - snowballs deal freeze damage.
 * Legacy: Uses PersistentDataContainer to mark snowballs, deals 1 freeze damage on hit.
 * Note: This requires ProjectileLaunchEvent and ProjectileHitEvent handling.
 * The v2 DSL doesn't have direct handlers for projectile events.
 * Implementation requires reactive event processor registration.
 */
val strongerSnowballs = ability("stronger_snowballs", "moborigins") {
    title = text("Stronger Snowballs")
    description("Snowballs you throw are packed with ice, and deal 1 damage!")

    option("damage", 1.0)
    option("knockback", 0.5)

    // Implementation note: Requires ProjectileLaunchEvent to mark snowballs
    // and ProjectileHitEvent to apply freeze damage and knockback.
    // Uses NMSInvoker.dealFreezeDamage and NMSInvoker.knockback
}

/**
 * Frigid Strength - deal more damage in cold biomes.
 * Legacy: Conditional AttributeModifierAbility - ATTACK_DAMAGE +3.0 when temperature < 0.15
 */
val frigidStrength = ability("frigid_strength", "moborigins") {
    title = text("Frigid Strength")
    description("Deal more damage in cold areas.")

    option("temperature_threshold", 0.15)
    option("damage_bonus", 3.0)
    option("attribute", "ATTACK_DAMAGE")
    option("operation", "ADD_NUMBER")

    // Conditional attribute based on biome temperature
    onTick(interval = 20) { player, config ->
        val threshold = config.getDouble("temperature_threshold", 0.15)
        player.location.block.temperature < threshold
    }
}

/**
 * Temperature - tracks temperature for melting mechanics.
 * Hidden ability that provides temperature tracking via cooldown display.
 * Legacy: Uses CooldownAbility to display temperature bar (0-100).
 */
val temperature = ability("temperature", "moborigins") {
    title = text("Temperature")
    description("Tracks your temperature level.")
    visible = false

    option("max_temperature", 100)

    // Temperature is tracked via playerTemperatureMap
    // The cooldown system should display this value
}

/**
 * Overheat - temperature increases in hot biomes.
 * Legacy: Every 20 ticks, adjusts temperature based on biome:
 *   - If block temperature < 1 OR standing on ice: decrease by 1
 *   - Otherwise: increase by 1
 * Also resets temperature on death and origin swap.
 */
val overheat = ability("overheat", "moborigins") {
    title = text("Overheat")
    description("You have a temperature bar that slowly begins to fill in hot biomes, and cool in other biomes.")

    option("check_interval", 20)
    option("hot_threshold", 1.0)

    onTick(interval = 20) { player, config ->
        val hotThreshold = config.getDouble("hot_threshold", 1.0)
        val location = player.location
        val block = location.block
        val belowBlockType = block.getRelative(BlockFace.DOWN).type
        val currentTemp = getTemperature(player)

        val newTemp = if (block.temperature < hotThreshold || Tag.ICE.isTagged(belowBlockType)) {
            currentTemp - 1
        } else {
            currentTemp + 1
        }
        setTemperature(player, newTemp)

        // Return whether temperature is actively changing (for potential UI updates)
        true
    }

    // Note: Temperature reset on death/origin swap requires event handlers
    // PlayerDeathEvent and PlayerSwapOriginEvent should reset temperature to 0
}

/**
 * Melting - lose health as temperature increases.
 * Legacy: Conditional AttributeModifierAbility based on temperature:
 *   - temperature >= 100: MAX_HEALTH -8.0
 *   - temperature >= 50: MAX_HEALTH -4.0
 *   - otherwise: no modifier
 */
val melting = ability("melting", "moborigins") {
    title = text("Melting")
    description("As your temperature bar fills up, you'll slowly begin to melt in hot biomes, losing health and speed.")

    option("health_reduction_50", -4.0)
    option("health_reduction_100", -8.0)
    option("attribute", "MAX_HEALTH")
    option("operation", "ADD_NUMBER")

    // Conditional attribute modifier based on temperature state
    onTick(interval = 20) { player, config ->
        val temp = getTemperature(player)
        // The tick handler returns true when modifier should be active
        // Processor should read temperature and apply appropriate reduction
        temp >= 50
    }
}

/**
 * Melting Speed - lose speed as temperature increases.
 * Hidden ability that works with Melting.
 * Legacy: Conditional AttributeModifierAbility based on temperature:
 *   - temperature >= 100: MOVEMENT_SPEED -0.04
 *   - temperature >= 50: MOVEMENT_SPEED -0.02
 *   - otherwise: no modifier
 */
val meltingSpeed = ability("melting_speed", "moborigins") {
    title = text("Melting Speed")
    description("Speed reduction from melting.")
    visible = false

    option("speed_reduction_50", -0.02)
    option("speed_reduction_100", -0.04)
    option("attribute", "MOVEMENT_SPEED")
    option("operation", "ADD_NUMBER")

    onTick(interval = 20) { player, _ ->
        val temp = getTemperature(player)
        temp >= 50
    }
}

/**
 * Collection of all snow and temperature-related abilities.
 */
val snowAbilities = listOf(
    snowTrail,
    strongerSnowballs,
    frigidStrength,
    temperature,
    overheat,
    melting,
    meltingSpeed
)
