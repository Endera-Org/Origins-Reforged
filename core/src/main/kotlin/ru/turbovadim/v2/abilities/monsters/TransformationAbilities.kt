package ru.turbovadim.v2.abilities.monsters

import org.bukkit.Bukkit
import org.bukkit.event.entity.EntityDamageEvent
import ru.turbovadim.v2.ability.DamageResult
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Transformation/Metamorphosis abilities for monster origins.
 * These abilities handle origin transformations based on environment conditions.
 *
 * Note: Transformation abilities require integration with:
 * - OriginSwapper.setOrigin() for actually changing the player's origin
 * - AddonLoader.getOrigin() for getting target origin
 * - MetamorphosisTemperature system for temperature tracking
 * - State management for time-based tracking (water duration, overworld time, etc.)
 */

// ============================================
// ZOMBIE TRANSFORMATIONS
// ============================================

/**
 * Drowned transforms into Zombie in warm areas.
 * Uses the MetamorphosisTemperature system - triggers when temperature >= 30.
 */
val drownedTransformIntoZombie = ability("drowned_transform_into_zombie", "monsterorigins") {
    title = text("Metamorphosis")
    description("You transform into a Zombie if you're in a warm area for too long.")

    option("warm_threshold", 30) // Temperature threshold to trigger transformation
    option("target_origin", "zombie")

    // Note: Full implementation requires MetamorphosisTemperature integration
    // Legacy behavior:
    // if (MetamorphosisTemperature.getTemperature(player) >= 30) {
    //     player.world.playSound(player, Sound.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED, SoundCategory.PLAYERS, 1f, 1f)
    //     OriginSwapper.setOrigin(player, AddonLoader.getOrigin("zombie"), SwapReason.PLUGIN, false, "origin")
    //     player.sendMessage(Component.text("You have transformed into a zombie!").color(NamedTextColor.YELLOW))
    // }
    onTick(interval = 20) { player, config ->
        val threshold = config.getInt("warm_threshold", 30)
        // Note: Would check MetamorphosisTemperature.getTemperature(player) >= threshold
        // and trigger transformation via OriginSwapper
        true
    }
}

/**
 * Husk transforms into Zombie when in water too long.
 * Tracks time underwater via EntityAirChangeEvent - triggers after 15 seconds (300 ticks).
 */
val huskTransformIntoZombie = ability("husk_transform_into_zombie", "monsterorigins") {
    title = text("Metamorphosis")
    description("You transform into a Zombie if you're in water for too long.")

    option("water_duration_ticks", 300) // 15 seconds
    option("target_origin", "zombie")

    // Note: Full implementation requires:
    // 1. EntityAirChangeEvent handling to track underwater time
    // 2. State management for lastOutOfAirTime per player
    // 3. Sets air amount to 0 (prevents drowning while counting)
    //
    // Legacy behavior:
    // On EntityAirChangeEvent:
    // if (event.amount > 0) {
    //     lastOutOfAirTime.remove(player)
    // } else {
    //     event.amount = 0  // Prevent drowning damage
    //     lastOutOfAirTime.putIfAbsent(player, Bukkit.getCurrentTick())
    //     if (Bukkit.getCurrentTick() - lastOutOfAirTime[player]!! >= 300) {
    //         switchToZombie(player)
    //         // Also sets MetamorphosisTemperature to min(70, currentTemp)
    //     }
    // }
}

/**
 * Zombie transforms into Husk (hot) or Drowned (water).
 * Uses MetamorphosisTemperature for Husk (>= 75) and underwater time for Drowned.
 */
val transformIntoHuskAndDrowned = ability("transform_into_husk_and_drowned", "monsterorigins") {
    title = text("Metamorphosis")
    description(
        "You transform into a Husk if you're in the desert for too long,",
        "and a Drowned if you're in the water for too long."
    )

    option("husk_temp_threshold", 75)
    option("water_duration_ticks", 300) // 15 seconds
    option("husk_origin", "husk")
    option("drowned_origin", "drowned")

    // Note: Full implementation requires:
    // 1. MetamorphosisTemperature integration for Husk transformation
    // 2. EntityAirChangeEvent handling for Drowned transformation
    //
    // Legacy behavior for Husk (onTick):
    // if (MetamorphosisTemperature.getTemperature(player) >= 75) {
    //     player.world.playSound(player, Sound.ENTITY_HUSK_CONVERTED_TO_ZOMBIE, ...)
    //     OriginSwapper.setOrigin(player, AddonLoader.getOrigin("husk"), ...)
    // }
    //
    // Legacy behavior for Drowned (EntityAirChangeEvent):
    // Same as HuskTransformIntoZombie but transforms to "drowned"
    // and sets MetamorphosisTemperature to min(20, currentTemp)
    onTick(interval = 20) { player, config ->
        val huskThreshold = config.getInt("husk_temp_threshold", 75)
        // Note: Would check MetamorphosisTemperature.getTemperature(player) >= huskThreshold
        // and trigger transformation to Husk
        true
    }
}

// ============================================
// SKELETON TRANSFORMATIONS
// ============================================

/**
 * Stray transforms into Skeleton in warm areas.
 * Uses MetamorphosisTemperature - triggers when temperature >= 30.
 */
val transformIntoSkeleton = ability("transform_into_skeleton", "monsterorigins") {
    title = text("Metamorphosis")
    description("You transform into a Skeleton if you're in a warm area for too long.")

    option("warm_threshold", 30)
    option("target_origin", "skeleton")

    // Note: Full implementation requires MetamorphosisTemperature integration
    // Legacy behavior:
    // if (MetamorphosisTemperature.getTemperature(player) >= 30) {
    //     player.world.playSound(player, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, ...)
    //     OriginSwapper.setOrigin(player, AddonLoader.getOrigin("skeleton"), ...)
    // }
    onTick(interval = 20) { player, config ->
        val threshold = config.getInt("warm_threshold", 30)
        // Note: Would check MetamorphosisTemperature.getTemperature(player) >= threshold
        true
    }
}

/**
 * Skeleton transforms into Stray in cold areas.
 * Uses freeze ticks OR MetamorphosisTemperature (<= 25) to trigger.
 * Also provides immunity to freeze damage.
 */
val transformIntoStray = ability("transform_into_stray", "monsterorigins") {
    title = text("Metamorphosis")
    description("You transform into a Stray if you're in the cold for too long.")

    option("freeze_duration_ticks", 300) // 15 seconds at max freeze
    option("cold_threshold", 25) // MetamorphosisTemperature threshold
    option("target_origin", "stray")

    // Freeze damage immunity
    modifyDamage(
        incoming = { _, _, cause, _ ->
            if (cause == EntityDamageEvent.DamageCause.FREEZE) {
                DamageResult.Cancel
            } else {
                DamageResult.Allow
            }
        }
    )

    // Note: Full implementation requires:
    // 1. Tracking lastHadLowFreezeTime per player
    // 2. MetamorphosisTemperature integration
    //
    // Legacy behavior:
    // if (player.freezeTicks < player.maxFreezeTicks) {
    //     lastHadLowFreezeTime[player] = Bukkit.getCurrentTick()
    // } else if (Bukkit.getCurrentTick() - lastHadLowFreezeTime.getOrDefault(player, now) >= 300) {
    //     MetamorphosisTemperature.setTemperature(player, 25)
    //     switchToStray(player)
    // } else if (MetamorphosisTemperature.getTemperature(player) <= 25) {
    //     switchToStray(player)
    // }
    onTick(interval = 20) { player, config ->
        val coldThreshold = config.getInt("cold_threshold", 25)
        // Note: Would check freeze time and temperature conditions
        true
    }
}

// ============================================
// PIGLIN TRANSFORMATIONS
// ============================================

/**
 * Piglin transforms into Zombified Piglin outside Nether.
 * Tracks time outside Nether - triggers after 15 seconds.
 */
val transformIntoZombifiedPiglin = ability("transform_into_zombified_piglin", "monsterorigins") {
    title = text("Metamorphosis")
    description("You transform into a Zombified Piglin if you're out of the Nether for too long.")

    option("overworld_duration_seconds", 15)
    option("nether_world_name", "world_nether")
    option("target_origin", "zombified piglin")

    // Note: Full implementation requires state management for overworldTime per player
    // Legacy behavior:
    // if (player.world === nether) {
    //     overworldTime[player] = 0
    // } else {
    //     overworldTime[player] = overworldTime.getOrDefault(player, 0) + 1
    // }
    // if (overworldTime.getOrDefault(player, 0) >= 15) {
    //     player.world.playSound(player, Sound.ENTITY_PIGLIN_CONVERTED_TO_ZOMBIFIED, ...)
    //     OriginSwapper.setOrigin(player, AddonLoader.getOrigin("zombified piglin"), ...)
    // }
    onTick(interval = 20) { player, config ->
        val durationSeconds = config.getInt("overworld_duration_seconds", 15)
        val netherName = config.getString("nether_world_name", "world_nether")
        val nether = Bukkit.getWorld(netherName)

        // Note: Would need state management to track time outside nether
        // and trigger transformation when threshold is reached
        val isInNether = player.world == nether
        // Reset or increment timer based on isInNether
        true
    }
}

/**
 * Zombified Piglin transforms into Piglin with golden apple + weakness.
 * Triggered by consuming a golden apple while having Weakness effect.
 */
val transformIntoPiglin = ability("transform_into_piglin", "monsterorigins") {
    title = text("Metamorphosis")
    description("You transform into a Piglin if you eat a golden apple when under the effect of a weakness potion.")

    option("target_origin", "piglin")

    // Note: Full implementation requires PlayerItemConsumeEvent handling
    // Legacy behavior:
    // if (event.item.type != Material.GOLDEN_APPLE) return
    // if (!player.hasPotionEffect(PotionEffectType.WEAKNESS)) return
    // player.world.playSound(player, Sound.ENTITY_PIGLIN_CONVERTED_TO_ZOMBIFIED, ...)
    // OriginSwapper.setOrigin(player, AddonLoader.getOrigin("piglin"), ...)
}

// ============================================
// METAMORPHOSIS TEMPERATURE SYSTEM
// ============================================

/**
 * Temperature tracking for metamorphosis transformations.
 * Tracks player temperature based on biome (0-100 scale, default 50).
 * Cold biomes (temp <= 0.15) decrease temperature.
 * Hot biomes (temp >= 1.75, not underwater) increase temperature.
 */
val metamorphosisTemperature = ability("metamorphosis_temperature", "monsterorigins") {
    title = text("Metamorphosis Temperature")
    description("Your body temperature changes based on the biome you're in.")
    visible = false // Hidden ability that tracks temperature

    option("cold_threshold", 0.15)
    option("hot_threshold", 1.75)
    option("check_interval", 20) // ticks
    option("default_temperature", 50)
    option("min_temperature", 0)
    option("max_temperature", 100)

    // Note: Full implementation requires:
    // 1. PersistentDataContainer storage for player temperature
    // 2. CooldownAbility integration for visual display
    // 3. PlayerSwapOriginEvent handling to reset temperature on origin change
    //
    // Legacy behavior:
    // val blockTemp = player.location.block.temperature
    // if (blockTemp <= 0.15) {
    //     setTemperature(player, getTemperature(player) - 1)
    // } else if (blockTemp >= 1.75 && !NMSInvoker.isUnderWater(player)) {
    //     setTemperature(player, getTemperature(player) + 1)
    // }
    //
    // Temperature storage uses NamespacedKey "player-temperature" in PersistentDataContainer
    // Values are clamped between 0 and 100
    onTick(interval = 20) { player, config ->
        val coldThreshold = config.getDouble("cold_threshold", 0.15)
        val hotThreshold = config.getDouble("hot_threshold", 1.75)

        val blockTemp = player.location.block.temperature

        // Note: Would need PersistentDataContainer access for temperature storage
        // if (blockTemp <= coldThreshold) {
        //     decrease temperature by 1
        // } else if (blockTemp >= hotThreshold && !player.isUnderWater) {
        //     increase temperature by 1
        // }
        true
    }
}
