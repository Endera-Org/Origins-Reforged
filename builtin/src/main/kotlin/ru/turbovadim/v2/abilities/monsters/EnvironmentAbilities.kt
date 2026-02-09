package ru.turbovadim.v2.abilities.monsters

import org.bukkit.entity.LivingEntity
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Environment-related abilities for monster origins.
 * Includes temperature effects, water abilities, and movement modifiers.
 */

// ============================================
// MOVEMENT SPEED MODIFIERS
// ============================================

/**
 * General slowness - undead movement penalty.
 * Uses AttributeModifier with MULTIPLY_SCALAR_1 operation (-15% speed).
 */
val slowness = ability("slowness", "monsterorigins") {
    title = text("Zombie Slowness")
    description("Your undead body moves at a slower pace than humans.")

    option("speed_reduction", -0.15)

    // Note: Attribute modifier application requires integration with AbilityManager
    // The attribute: MOVEMENT_SPEED, amount: -0.15, operation: MULTIPLY_SCALAR_1
}

/**
 * Land slowness - slow on land, normal in water.
 * Speed reduced by 20% when not in water.
 */
val landSlowness = ability("land_slowness", "monsterorigins") {
    title = text("Water Based")
    description("You're used to the water, so move much slower on land.")

    option("land_speed_reduction", -0.2)

    // Note: Attribute modifier with dynamic amount requires tick-based checking
    // Legacy behavior: getChangedAmount returns -0.2 when not in water, 0.0 when in water
    // Attribute: MOVEMENT_SPEED, operation: MULTIPLY_SCALAR_1
    onTick(interval = 5) { player, config ->
        // This would need to dynamically update the attribute modifier
        // based on player.isInWater state. The actual implementation
        // requires attribute modifier management in AbilityManager.
        true
    }
}

/**
 * Heat slowness - slowed in warm biomes.
 * Speed reduction based on biome temperature.
 */
val heatSlowness = ability("heat_slowness", "monsterorigins") {
    title = text("Cold Body")
    description("Your cold body conflicts with warmer biomes, slowing you down.")

    option("hot_temp_threshold_2", 2.0)
    option("hot_temp_threshold_1_5", 1.5)
    option("hot_temp_threshold_1", 1.0)
    option("hot_temp_threshold_0_5", 0.5)

    // Note: Attribute modifier with dynamic amount based on biome temperature
    // Legacy behavior based on temperature thresholds:
    // temp >= 2.0 -> -0.2 speed
    // temp >= 1.5 -> -0.15 speed
    // temp >= 1.0 -> -0.1 speed
    // temp >= 0.5 -> -0.05 speed
    // else -> 0.0 (no reduction)
    onTick(interval = 5) { player, config ->
        val temp = player.location.block.temperature
        val reduction = when {
            temp >= config.getDouble("hot_temp_threshold_2", 2.0) -> -0.2
            temp >= config.getDouble("hot_temp_threshold_1_5", 1.5) -> -0.15
            temp >= config.getDouble("hot_temp_threshold_1", 1.0) -> -0.1
            temp >= config.getDouble("hot_temp_threshold_0_5", 0.5) -> -0.05
            else -> 0.0
        }
        // Note: This would need to update attribute modifier dynamically
        // Attribute: MOVEMENT_SPEED, operation: MULTIPLY_SCALAR_1
        true
    }
}

/**
 * Cold slowness - slowed in cold biomes.
 * Speed reduction based on biome temperature.
 */
val coldSlowness = ability("cold_slowness", "monsterorigins") {
    title = text("Warm Body")
    description("Your warm body conflicts with colder biomes, slowing you down.")

    option("cold_temp_threshold_0", 0.0)
    option("cold_temp_threshold_0_5", 0.5)
    option("cold_temp_threshold_1", 1.0)
    option("cold_temp_threshold_1_5", 1.5)

    // Note: Attribute modifier with dynamic amount based on biome temperature
    // Legacy behavior based on temperature thresholds:
    // temp <= 0.0 -> -0.2 speed
    // temp <= 0.5 -> -0.15 speed
    // temp <= 1.0 -> -0.1 speed
    // temp <= 1.5 -> -0.05 speed
    // else -> 0.0 (no reduction)
    onTick(interval = 5) { player, config ->
        val temp = player.location.block.temperature
        val reduction = when {
            temp <= config.getDouble("cold_temp_threshold_0", 0.0) -> -0.2
            temp <= config.getDouble("cold_temp_threshold_0_5", 0.5) -> -0.15
            temp <= config.getDouble("cold_temp_threshold_1", 1.0) -> -0.1
            temp <= config.getDouble("cold_temp_threshold_1_5", 1.5) -> -0.05
            else -> 0.0
        }
        // Note: This would need to update attribute modifier dynamically
        // Attribute: MOVEMENT_SPEED, operation: MULTIPLY_SCALAR_1
        true
    }
}

// ============================================
// WATER ABILITIES
// ============================================

/**
 * Water breathing - can breathe underwater.
 * Keeps air at maximum when underwater.
 */
val waterBreathingMonsters = ability("water_breathing", "monsterorigins") {
    title = text("Water Breathing")
    description("You can breathe underwater.")
    visible = false

    // Note: Full implementation requires EntityAirChangeEvent handling
    // Legacy behavior: event.amount = player.maximumAir
    // This prevents air from decreasing underwater
    onTick(interval = 1) { player, _ ->
        if (player.isInWater && player.remainingAir < player.maximumAir) {
            player.remainingAir = player.maximumAir
        }
        true
    }
}

/**
 * Fast swimmer - increased underwater speed via Dolphin's Grace.
 * Applies Dolphin's Grace with amplifier -1 when underwater.
 */
val swimSpeedMonsters = ability("swim_speed", "monsterorigins") {
    title = text("Fast Swimmer")
    description("Your underwater speed is increased.")

    onTick(interval = 1) { player, _ ->
        // Note: Full implementation would need to store existing dolphin's grace
        // and restore it when leaving water. Uses amplifier -1 as marker.
        if (player.isUnderWater) {
            val effect = player.getPotionEffect(PotionEffectType.DOLPHINS_GRACE)
            val ambient = effect?.isAmbient ?: false
            val showParticles = effect?.hasParticles() ?: false

            // Store existing non-ability effect if needed (requires state management)
            // Apply ability effect with amplifier -1
            player.addPotionEffect(
                PotionEffect(
                    PotionEffectType.DOLPHINS_GRACE,
                    Int.MAX_VALUE,
                    -1, // Special amplifier to identify ability-granted effect
                    ambient,
                    showParticles
                )
            )
        } else {
            // Remove ability-granted effect when not underwater
            val effect = player.getPotionEffect(PotionEffectType.DOLPHINS_GRACE)
            if (effect != null && effect.amplifier == -1) {
                player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE)
            }
        }
        true
    }
}

// ============================================
// HUNGER/SATURATION
// ============================================

/**
 * Zombie hunger - exhaust faster.
 * Multiplies exhaustion by 1.5x.
 */
val zombieHunger = ability("zombie_hunger", "monsterorigins") {
    title = text("Zombie Hunger")
    description("Your constant hunger for flesh makes you exhaust quicker than a human.")

    option("exhaustion_multiplier", 1.5)

    // Note: Full implementation requires EntityExhaustionEvent handling
    // Legacy behavior: event.exhaustion = event.exhaustion * 1.5f
    // The v2 DSL doesn't have a direct exhaustion handler yet
}

/**
 * Half max saturation - can only store half saturation.
 * Caps saturation at half of food level.
 */
val halfMaxSaturation = ability("half_max_saturation", "monsterorigins") {
    title = text("Poor Digestion")
    description("You can only hold half as much saturation as a human.")

    // Note: Full implementation requires FoodLevelChangeEvent handling
    // Legacy behavior: player.saturation = min(player.saturation, player.foodLevel / 2f)
    onTick(interval = 1) { player, _ ->
        val maxSaturation = player.foodLevel.toFloat() / 2f
        if (player.saturation > maxSaturation) {
            player.saturation = maxSaturation
        }
        true
    }
}

// ============================================
// SENSE ABILITIES
// ============================================

/**
 * Sense movement - see nearby entities through walls via glowing effect.
 * Uses packet-based entity data to show glowing to specific players.
 */
val senseMovement = ability("sense_movement", "monsterorigins") {
    title = text("Heightened Senses")
    description("You can see the outlines of nearby mobs, even through blocks.")

    option("detection_radius", 24.0)
    option("glow_radius", 16.0)
    option("check_interval", 20)

    onTick(interval = 20) { player, config ->
        val detectionRadius = config.getDouble("detection_radius", 24.0)
        val glowRadius = config.getDouble("glow_radius", 16.0)

        // Note: Full implementation requires NMSInvoker.sendEntityData to send
        // per-player entity metadata packets. This sets the glowing flag (0x40)
        // for entities within glow_radius, preserving other entity state flags.
        //
        // Legacy behavior builds entity data byte with flags:
        // - 0x40: Glowing (if within glow_radius or already glowing)
        // - 0x01: On fire
        // - 0x20: Invisible
        // - 0x02: Sneaking (players only)
        // - 0x08: Sprinting (players only)
        // - 0x10: Swimming (players only)
        // - 0x80: Gliding (players only)
        //
        // Then calls NMSInvoker.sendEntityData(player, entity, data)

        for (entity in player.getNearbyEntities(detectionRadius, detectionRadius, detectionRadius)) {
            if (entity === player) continue
            if (entity !is LivingEntity) continue

            // Note: Actual packet sending requires NMS access
            // This is a placeholder showing the logic
            val distance = entity.location.distance(player.location)
            val shouldGlow = entity.isGlowing || distance <= glowRadius
            // Would send entity data packet here
        }
        true
    }
}
