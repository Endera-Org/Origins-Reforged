package ru.turbovadim.v2.abilities.monsters

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityAirChangeEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.ability.DamageResult
import ru.turbovadim.v2.api.OriginsApi
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/**
 * Metamorphosis / transformation abilities for monster origins.
 *
 * Temperature is tracked via PersistentDataContainer (0-100, default 50).
 * Cold biomes decrease it; hot biomes above water increase it.
 */

private val TEMPERATURE_KEY: NamespacedKey by lazy {
    NamespacedKey(OriginsReforged.instance, "player-temperature")
}

fun getMetamorphosisTemperature(player: Player): Int {
    return player.persistentDataContainer.getOrDefault(TEMPERATURE_KEY, PersistentDataType.INTEGER, 50)
}

fun setMetamorphosisTemperature(player: Player, amount: Int) {
    player.persistentDataContainer.set(
        TEMPERATURE_KEY,
        PersistentDataType.INTEGER,
        max(0, min(amount, 100))
    )
}

private val lastUnderwaterTick: MutableMap<UUID, Int> = HashMap()
private val lastLowFreezeTick: MutableMap<UUID, Int> = HashMap()
private val overworldTicks: MutableMap<UUID, Int> = HashMap()

private fun switchTo(player: Player, originName: String, sound: Sound, message: String) {
    val api = OriginsApi.getOrNull() ?: return
    val origin = api.getOriginByName(originName) ?: return
    player.world.playSound(player, sound, SoundCategory.PLAYERS, 1f, 1f)
    api.setPlayerOrigin(player, origin.layer, origin)
    player.sendMessage(Component.text(message, NamedTextColor.YELLOW))
}

val metamorphosisTemperature = ability("metamorphosis_temperature", "monsterorigins") {
    title = text("Metamorphosis Temperature")
    description("Your body temperature changes based on the biome you're in.")
    visible = false

    option("cold_threshold", 0.15)
    option("hot_threshold", 1.75)

    onTick(interval = 20) { player, config ->
        val coldThreshold = config.getDouble("cold_threshold", 0.15)
        val hotThreshold = config.getDouble("hot_threshold", 1.75)
        val blockTemp = player.location.block.temperature
        val current = getMetamorphosisTemperature(player)
        when {
            blockTemp <= coldThreshold -> setMetamorphosisTemperature(player, current - 1)
            blockTemp >= hotThreshold && !OriginsReforged.NMSInvoker.isUnderWater(player) ->
                setMetamorphosisTemperature(player, current + 1)
        }
        true
    }

    onOriginChanged { player, _, _ ->
        setMetamorphosisTemperature(player, 50)
    }
}

val drownedTransformIntoZombie = ability("drowned_transform_into_zombie", "monsterorigins") {
    title = text("Metamorphosis")
    description("You transform into a Zombie if you're in a warm area for too long.")

    option("warm_threshold", 30)
    option("target_origin", "zombie")

    onTick(interval = 20) { player, config ->
        if (getMetamorphosisTemperature(player) >= config.getInt("warm_threshold", 30)) {
            switchTo(
                player,
                config.getString("target_origin", "zombie"),
                Sound.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED,
                "You have transformed into a zombie!"
            )
        }
        true
    }
}

val huskTransformIntoZombie = ability("husk_transform_into_zombie", "monsterorigins") {
    title = text("Metamorphosis")
    description("You transform into a Zombie if you're in water for too long.")

    option("water_duration_ticks", 300)
    option("target_origin", "zombie")
    option("temperature_cap", 70)

    listener<EntityAirChangeEvent>(
        ignoreCancelled = false,
        playerFrom = { it.entity as? Player }
    ) { player, event, config ->
        if (event.amount > 0) {
            lastUnderwaterTick.remove(player.uniqueId)
            return@listener
        }
        event.amount = 0
        val currentTick = Bukkit.getCurrentTick()
        val start = lastUnderwaterTick.getOrPut(player.uniqueId) { currentTick }
        val threshold = config.getInt("water_duration_ticks", 300)
        if (currentTick - start >= threshold) {
            lastUnderwaterTick.remove(player.uniqueId)
            val cap = config.getInt("temperature_cap", 70)
            setMetamorphosisTemperature(player, min(cap, getMetamorphosisTemperature(player)))
            switchTo(
                player,
                config.getString("target_origin", "zombie"),
                Sound.ENTITY_HUSK_CONVERTED_TO_ZOMBIE,
                "You have transformed into a zombie!"
            )
        }
    }
}

val transformIntoHuskAndDrowned = ability("transform_into_husk_and_drowned", "monsterorigins") {
    title = text("Metamorphosis")
    description(
        "You transform into a Husk if you're in the desert for too long,",
        "and a Drowned if you're in the water for too long."
    )

    option("husk_temp_threshold", 75)
    option("water_duration_ticks", 300)
    option("husk_origin", "husk")
    option("drowned_origin", "drowned")
    option("drowned_temperature_cap", 20)

    onTick(interval = 20) { player, config ->
        if (getMetamorphosisTemperature(player) >= config.getInt("husk_temp_threshold", 75)) {
            switchTo(
                player,
                config.getString("husk_origin", "husk"),
                Sound.ENTITY_HUSK_CONVERTED_TO_ZOMBIE,
                "You have transformed into a husk!"
            )
        }
        true
    }

    listener<EntityAirChangeEvent>(
        ignoreCancelled = false,
        playerFrom = { it.entity as? Player }
    ) { player, event, config ->
        if (event.amount > 0) {
            lastUnderwaterTick.remove(player.uniqueId)
            return@listener
        }
        event.amount = 0
        val currentTick = Bukkit.getCurrentTick()
        val start = lastUnderwaterTick.getOrPut(player.uniqueId) { currentTick }
        val threshold = config.getInt("water_duration_ticks", 300)
        if (currentTick - start >= threshold) {
            lastUnderwaterTick.remove(player.uniqueId)
            val cap = config.getInt("drowned_temperature_cap", 20)
            setMetamorphosisTemperature(player, min(cap, getMetamorphosisTemperature(player)))
            switchTo(
                player,
                config.getString("drowned_origin", "drowned"),
                Sound.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED,
                "You have transformed into a drowned!"
            )
        }
    }
}

val transformIntoSkeleton = ability("transform_into_skeleton", "monsterorigins") {
    title = text("Metamorphosis")
    description("You transform into a Skeleton if you're in a warm area for too long.")

    option("warm_threshold", 30)
    option("target_origin", "skeleton")

    onTick(interval = 20) { player, config ->
        if (getMetamorphosisTemperature(player) >= config.getInt("warm_threshold", 30)) {
            switchTo(
                player,
                config.getString("target_origin", "skeleton"),
                Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY,
                "You have transformed into a skeleton!"
            )
        }
        true
    }
}

val transformIntoStray = ability("transform_into_stray", "monsterorigins") {
    title = text("Metamorphosis")
    description("You transform into a Stray if you're in the cold for too long.")

    option("freeze_duration_ticks", 300)
    option("cold_threshold", 25)
    option("target_origin", "stray")

    modifyDamage(
        incoming = { _, _, cause, _ ->
            if (cause == EntityDamageEvent.DamageCause.FREEZE) DamageResult.Cancel
            else DamageResult.Allow
        }
    )

    onTick(interval = 20) { player, config ->
        val currentTick = Bukkit.getCurrentTick()
        val freezeThreshold = config.getInt("freeze_duration_ticks", 300)
        val coldThreshold = config.getInt("cold_threshold", 25)

        if (player.freezeTicks < player.maxFreezeTicks) {
            lastLowFreezeTick[player.uniqueId] = currentTick
        } else {
            val lastLow = lastLowFreezeTick.getOrDefault(player.uniqueId, currentTick)
            if (currentTick - lastLow >= freezeThreshold) {
                setMetamorphosisTemperature(player, coldThreshold)
                switchTo(
                    player,
                    config.getString("target_origin", "stray"),
                    Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY,
                    "You have transformed into a stray!"
                )
                return@onTick true
            }
        }

        if (getMetamorphosisTemperature(player) <= coldThreshold) {
            switchTo(
                player,
                config.getString("target_origin", "stray"),
                Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY,
                "You have transformed into a stray!"
            )
        }
        true
    }
}

val transformIntoZombifiedPiglin = ability("transform_into_zombified_piglin", "monsterorigins") {
    title = text("Metamorphosis")
    description("You transform into a Zombified Piglin if you're out of the Nether for too long.")

    option("overworld_duration_seconds", 15)
    option("target_origin", "zombified piglin")

    onTick(interval = 20) { player, config ->
        val netherName = OriginsReforged.mainConfig.worlds.worldNether
        val nether = Bukkit.getWorld(netherName)
        val threshold = config.getInt("overworld_duration_seconds", 15)

        if (nether != null && player.world === nether) {
            overworldTicks[player.uniqueId] = 0
        } else {
            val current = overworldTicks.getOrDefault(player.uniqueId, 0) + 1
            overworldTicks[player.uniqueId] = current
            if (current >= threshold) {
                overworldTicks[player.uniqueId] = 0
                switchTo(
                    player,
                    config.getString("target_origin", "zombified piglin"),
                    Sound.ENTITY_PIGLIN_CONVERTED_TO_ZOMBIFIED,
                    "You have transformed into a Zombified Piglin!"
                )
            }
        }
        true
    }
}

val transformIntoPiglin = ability("transform_into_piglin", "monsterorigins") {
    title = text("Metamorphosis")
    description("You transform into a Piglin if you eat a golden apple when under the effect of a weakness potion.")

    option("target_origin", "piglin")

    listener<PlayerItemConsumeEvent>(
        ignoreCancelled = false,
        playerFrom = { it.player }
    ) { player, event, config ->
        if (event.item.type != Material.GOLDEN_APPLE) return@listener
        if (!player.hasPotionEffect(PotionEffectType.WEAKNESS)) return@listener
        switchTo(
            player,
            config.getString("target_origin", "piglin"),
            Sound.ENTITY_PIGLIN_CONVERTED_TO_ZOMBIFIED,
            "You have transformed into a Piglin!"
        )
    }
}
