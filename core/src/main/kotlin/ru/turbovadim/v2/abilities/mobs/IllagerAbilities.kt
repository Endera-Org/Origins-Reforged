package ru.turbovadim.v2.abilities.mobs

import org.bukkit.NamespacedKey
import org.bukkit.block.BlockFace
import org.bukkit.entity.EntityType
import org.bukkit.persistence.PersistentDataType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text
import java.util.*

/**
 * Illager-related abilities for the Mobs module.
 * Used by evoker, pillager, and similar illager origins.
 */

// Random instance for totem chance calculations
private val random = Random()

// Illager entity types that won't target players with illager ability
private val ILLAGER_TYPES = listOf(
    EntityType.ILLUSIONER,
    EntityType.EVOKER,
    EntityType.VINDICATOR,
    EntityType.RAVAGER,
    EntityType.WITCH,
    EntityType.VEX,
    EntityType.PILLAGER
)

/**
 * Illager - illagers don't attack you.
 * Legacy: On EntityTargetLivingEntityEvent when entity is an illager type:
 *   - Cancels event if target has this ability
 */
val illager = ability("illager", "moborigins") {
    title = text("Illager")
    description("Illagers won't attack you.")
    visible = false

    // Implementation note: Requires EntityTargetLivingEntityEvent handler
    // When entity.type in ILLAGER_TYPES and target has ability:
    // event.isCancelled = true
}

/**
 * Pillager Aligned - villagers don't like you, pillagers do.
 * Legacy: Multiple event handlers:
 *   - EntityTargetLivingEntityEvent: Cancels if PILLAGER targets player with ability
 *   - PlayerInteractEntityEvent: Cancels villager trading, makes villager shake head
 *   - EntitySpawnEvent/EntitiesLoadEvent: Adds mob goal to iron golems to attack players with ability
 * Note: Requires multiple event handlers and NMSInvoker for golem goal.
 */
val pillagerAligned = ability("pillager_aligned", "moborigins") {
    title = text("Pillager Aligned")
    description("Villagers don't like you, and pillagers like you!")

    // Implementation notes:
    // 1. EntityTargetLivingEntityEvent: Cancel if PILLAGER targets player with ability
    // 2. PlayerInteractEntityEvent: If right-clicked entity is Villager:
    //    - Cancel event
    //    - villager.shakeHead()
    // 3. EntitySpawnEvent/EntitiesLoadEvent for IronGolem:
    //    - Bukkit.getMobGoals().addGoal(golem, 3, NMSInvoker.getIronGolemAttackGoal(golem) { hasAbility(it) })
}

/**
 * Summon Fangs - summon evoker fangs in a line.
 * Legacy: On PlayerLeftClickEvent with empty hand:
 *   - Spawns 16 evoker fangs in a line in player's facing direction
 *   - Fangs are tagged with player UUID to prevent self-damage
 *   - Skips positions where block below is not solid
 */
val summonFangs = ability("summon_fangs", "moborigins") {
    title = text("Summon Fangs")
    description("You have the ability to summon fangs!")

    option("cooldown_ticks", 600)
    option("fang_count", 16)

    onPrimaryAction { player, config ->
        if (!player.inventory.itemInMainHand.type.isAir) return@onPrimaryAction

        val fangCount = config.getInt("fang_count", 16)
        val currentLoc = player.location.clone()
        val horizontalDir = currentLoc.direction.setY(0).normalize()

        // Key for tracking which fangs belong to this player (for self-damage prevention)
        val sentFromPlayerKey = NamespacedKey.fromString("sent-from-player")!!

        for (i in 0 until fangCount) {
            currentLoc.add(horizontalDir)

            // Only spawn if there's solid ground below
            if (!currentLoc.block.getRelative(BlockFace.DOWN).isSolid) continue

            val fangs = currentLoc.world.spawnEntity(currentLoc, EntityType.EVOKER_FANGS)
            // Tag with player UUID for self-damage prevention
            fangs.persistentDataContainer.set(
                sentFromPlayerKey,
                PersistentDataType.STRING,
                player.uniqueId.toString()
            )
        }

        // Note: Legacy also has EntityDamageByEntityEvent handler to cancel damage
        // when fangs tagged with player's UUID damage that same player
    }
}

/**
 * Lower Totem Chance - totems have a chance not to break on use.
 * Legacy: On EntityResurrectEvent:
 *   - 10% chance (random.nextDouble() < 0.1) to restore the totem after use
 *   - Creates new totem and places in same hand (main or off)
 * Note: Requires EntityResurrectEvent handler.
 */
val lowerTotemChance = ability("lower_totem_chance", "moborigins") {
    title = text("Arcane Totems")
    description("Totems have a 10% chance not to break on use.")

    option("save_chance", 0.1)

    // Implementation note: Requires EntityResurrectEvent handler
    // When player resurrects:
    // if (random.nextDouble() < save_chance):
    //   val newTotem = ItemStack(Material.TOTEM_OF_UNDYING)
    //   if (equipment.itemInMainHand.type == TOTEM_OF_UNDYING):
    //     equipment.setItemInMainHand(newTotem)
    //   else:
    //     equipment.setItemInOffHand(newTotem)
}

/**
 * Collection of all illager-related abilities.
 */
val illagerAbilities = listOf(
    illager,
    pillagerAligned,
    summonFangs,
    lowerTotemChance
)
