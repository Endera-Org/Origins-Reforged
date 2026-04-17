package ru.turbovadim.v2.abilities.mobs

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.BlockFace
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityResurrectEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.api.OriginsApi
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import java.util.Random

/**
 * Illager-related abilities for the Mobs module.
 */

private val random = Random()

private val ILLAGER_TYPES = setOf(
    EntityType.ILLUSIONER,
    EntityType.EVOKER,
    EntityType.VINDICATOR,
    EntityType.RAVAGER,
    EntityType.WITCH,
    EntityType.VEX,
    EntityType.PILLAGER
)

private val fangsOwnerKey: NamespacedKey by lazy {
    NamespacedKey(OriginsReforged.instance, "sent-from-player")
}

/**
 * Illager - illagers don't attack you.
 */
val illager = ability("illager", "moborigins") {
    title = text("Illager")
    description("Illagers won't attack you.")
    visible = false

    onEntityTarget { _, attacker, _ ->
        attacker.type !in ILLAGER_TYPES
    }
}

/**
 * Pillager Aligned - villagers don't like you, pillagers do.
 */
val pillagerAligned = ability("pillager_aligned", "moborigins") {
    title = text("Pillager Aligned")
    description("Villagers don't like you, and pillagers like you!")

    onEntityTarget { _, attacker, _ ->
        attacker.type != EntityType.PILLAGER
    }

    listener<PlayerInteractEntityEvent>(
        ignoreCancelled = false,
        playerFrom = { it.player }
    ) { _, event, _ ->
        val villager = event.rightClicked as? Villager ?: return@listener
        event.isCancelled = true
        villager.shakeHead()
    }
}

/**
 * Summon Fangs - summon evoker fangs in a line.
 */
val summonFangs = ability("summon_fangs", "moborigins") {
    title = text("Summon Fangs")
    description("You have the ability to summon fangs!")

    option("cooldown_ticks", 600)
    option("fang_count", 16)

    onPrimaryAction { player, config ->
        if (!player.inventory.itemInMainHand.type.isAir) return@onPrimaryAction

        val api = OriginsApi.getOrNull()
        val abilityKey = net.kyori.adventure.key.Key.key("moborigins", "summon_fangs")
        if (api != null && api.hasCooldown(player, abilityKey)) return@onPrimaryAction

        val fangCount = config.getInt("fang_count", 16)
        val currentLoc = player.location.clone()
        val horizontalDir = currentLoc.direction.setY(0).normalize()

        for (i in 0 until fangCount) {
            currentLoc.add(horizontalDir)

            if (!currentLoc.block.getRelative(BlockFace.DOWN).isSolid) continue

            val fangs = currentLoc.world.spawnEntity(currentLoc, EntityType.EVOKER_FANGS)
            fangs.persistentDataContainer.set(
                fangsOwnerKey,
                PersistentDataType.STRING,
                player.uniqueId.toString()
            )
        }

        api?.setCooldown(player, abilityKey, config.getInt("cooldown_ticks", 600), "evoker_fangs")
    }

    // Prevent fangs from damaging their summoner
    listener<EntityDamageByEntityEvent>(
        playerFrom = { event ->
            if (event.damager.type != EntityType.EVOKER_FANGS) return@listener null
            event.entity as? Player
        }
    ) { player, event, _ ->
        val storedUuid = event.damager.persistentDataContainer
            .get(fangsOwnerKey, PersistentDataType.STRING) ?: return@listener
        if (storedUuid == player.uniqueId.toString()) {
            event.isCancelled = true
        }
    }
}

/**
 * Lower Totem Chance - totems have a chance not to break on use.
 */
val lowerTotemChance = ability("lower_totem_chance", "moborigins") {
    title = text("Arcane Totems")
    description("Totems have a 10% chance not to break on use.")

    option("save_chance", 0.1)

    listener<EntityResurrectEvent>(
        ignoreCancelled = false,
        playerFrom = { it.entity as? Player }
    ) { player, _, config ->
        val chance = config.getDouble("save_chance", 0.1)
        if (random.nextDouble() >= chance) return@listener

        val newTotem = ItemStack(Material.TOTEM_OF_UNDYING)
        val equipment = player.equipment

        Bukkit.getScheduler().runTaskLater(OriginsReforged.instance, Runnable {
            if (equipment.itemInMainHand.type == Material.TOTEM_OF_UNDYING) {
                equipment.setItemInMainHand(newTotem)
            } else {
                equipment.setItemInOffHand(newTotem)
            }
        }, 1L)
    }
}

val illagerAbilities = listOf(
    illager,
    pillagerAligned,
    summonFangs,
    lowerTotemChance
)
