package ru.turbovadim.v2.abilities.magic

import org.bukkit.Bukkit
import org.bukkit.GameEvent
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.world.GenericGameEvent
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import kotlin.math.abs

/**
 * Spirit origin abilities ported from Origins-Magic (StarshooterCity).
 *
 * Includes:
 *   - silent: Immune to sculk sensor vibrations for most game events.
 *   - invisible_when_still: Fully invisible when you've stood still for a moment.
 */

/**
 * Spectral - Silences the player's footsteps, eating and drinking for sculk sensors.
 *
 * Specifically cancels [GenericGameEvent] for the events in the original list:
 * STEP, ITEM_INTERACT_FINISH, ITEM_INTERACT_START, ENTITY_DAMAGE, EAT, DRINK, HIT_GROUND.
 */
private val SILENT_EVENTS: Set<GameEvent> = setOf(
    GameEvent.STEP,
    GameEvent.ITEM_INTERACT_FINISH,
    GameEvent.ITEM_INTERACT_START,
    GameEvent.ENTITY_DAMAGE,
    GameEvent.EAT,
    GameEvent.DRINK,
    GameEvent.HIT_GROUND
)

val silent = ability("silent", "magicorigins") {
    title = text("Spectral")
    description("Your ghost-like nature prevents you being detected by Sculk Sensors when you move or eat.")

    listener<GenericGameEvent>(
        ignoreCancelled = false,
        playerFrom = { it.entity as? Player }
    ) { _, event, _ ->
        if (event.event in SILENT_EVENTS) event.isCancelled = true
    }
}

/**
 * Ghostly - Player becomes fully invisible if they stand still for more than 4 ticks.
 * Tracked using [StillnessTracker] (shared with [regenerationWhenStill]).
 *
 * Also cancels mob targeting while still - matches the legacy [EntityMoveEvent] handler
 * that drops a mob's target the instant the player stops moving.
 */
val invisibleWhenStill = ability("invisible_when_still", "magicorigins") {
    title = text("Ghostly")
    description("You can stand so still that you can't be seen.")

    invisibleWhen { player ->
        val lastMoved = StillnessTracker.lastMovedTick(player)
        lastMoved + 4 <= Bukkit.getCurrentTick()
    }

    // Also track movement here so it still works if regenerationWhenStill isn't on the player
    listener<PlayerMoveEvent>(
        ignoreCancelled = false,
        playerFrom = { it.player }
    ) { player, event, _ ->
        if (!event.hasExplicitlyChangedPosition()) return@listener
        val from = event.from
        val to = event.to
        val motionX = abs(from.x - to.x)
        val motionZ = abs(from.z - to.z)
        val motionY = abs(from.y - to.y)
        if (motionX < 0.05 && motionZ < 0.05 && motionY < 0.05) return@listener
        StillnessTracker.markMoved(player)
    }

    // If a mob targets a still player, drop the target
    listener<EntityTargetEvent>(
        ignoreCancelled = false,
        playerFrom = { it.target as? Player }
    ) { player, event, _ ->
        val lastMoved = StillnessTracker.lastMovedTick(player)
        if (lastMoved + 4 <= Bukkit.getCurrentTick()) {
            event.isCancelled = true
            if (event.entity is Mob) (event.entity as Mob).target = null
        }
    }
}

val spiritAbilities = listOf(
    silent,
    invisibleWhenStill
)
