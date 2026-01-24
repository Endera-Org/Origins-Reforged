package ru.turbovadim.v2.processor

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import ru.turbovadim.v2.ability.AbilityEffect
import ru.turbovadim.v2.di.OriginsContainer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Batched processor for periodic ability effects.
 *
 * This SINGLE processor replaces 78+ individual ServerTickEndEvent listeners.
 * Effects are grouped by their tick interval for efficient batch processing.
 *
 * Performance improvements:
 * - Single tick listener instead of 78+
 * - Effects grouped by interval (process once per interval, not every tick)
 * - Players grouped for single iteration
 * - Async processing for expensive operations (block lookups)
 */
class PeriodicAbilityProcessor(private val container: OriginsContainer) : Listener {

    private val scope = CoroutineScope(container.dispatchers.compute + SupervisorJob())

    // Current tick counter
    private val tickCounter = AtomicLong(0)

    // Player -> their periodic tasks
    private val playerTasks = ConcurrentHashMap<UUID, MutableSet<PeriodicTask>>()

    // Tick interval -> tasks that run at this interval
    private val tickBuckets = ConcurrentHashMap<Int, MutableSet<PeriodicTask>>()

    // Scheduled task handle for cleanup
    private var scheduledTask: ScheduledTask? = null

    /**
     * A single periodic task for a player's ability.
     */
    data class PeriodicTask(
        val playerId: UUID,
        val abilityKey: Key,
        val effect: AbilityEffect.Periodic
    )

    /**
     * Start the periodic processor.
     */
    fun start() {
        scheduledTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
            container.plugin,
            { onServerTick() },
            1L,
            1L
        )
    }

    fun stop() {
        scheduledTask?.cancel()
        scheduledTask = null
        playerTasks.clear()
        tickBuckets.clear()
    }
    /**
     * Update periodic tasks for a player.
     * Called when their abilities change.
     */

    fun updatePlayer(playerId: UUID, abilityKeys: Set<Key>) {
        // Remove old tasks
        removePlayer(playerId)

        // Build new tasks from abilities
        val newTasks = mutableSetOf<PeriodicTask>()

        for (abilityKey in abilityKeys) {
            val periodicEffects = container.abilityRegistry.getPeriodicEffects(abilityKey)

            for (effect in periodicEffects) {
                val task = PeriodicTask(playerId, abilityKey, effect)
                newTasks.add(task)

                // Add to tick bucket
                tickBuckets.getOrPut(effect.intervalTicks) { ConcurrentHashMap.newKeySet() }.add(task)
            }
        }

        if (newTasks.isNotEmpty()) {
            playerTasks[playerId] = newTasks
        }
    }

    /**
     * Remove all periodic tasks for a player.
     * Called when they disconnect or lose all abilities.
     */
    fun removePlayer(playerId: UUID) {
        val tasks = playerTasks.remove(playerId) ?: return

        for (task in tasks) {
            tickBuckets[task.effect.intervalTicks]?.remove(task)
        }
    }

    /**
     * Single tick handler - replaces 78+ individual handlers.
     */
    private fun onServerTick() {
        val tick = tickCounter.incrementAndGet()

        // Process each tick bucket that should fire this tick
        for ((interval, tasks) in tickBuckets) {
            if (tick % interval == 0L && tasks.isNotEmpty()) {
                processBatch(tasks.toSet()) // Copy to avoid concurrent modification
            }
        }
    }

    /**
     * Process a batch of tasks efficiently.
     */
    private fun processBatch(tasks: Set<PeriodicTask>) {
        val potionTasks = mutableListOf<PeriodicTask>()
        val envCheckTasks = mutableListOf<PeriodicTask>()
        val particleTasks = mutableListOf<PeriodicTask>()

        for (task in tasks) {
            when (task.effect) {
                is AbilityEffect.Periodic.ApplyPotion -> potionTasks.add(task)
                is AbilityEffect.Periodic.EnvironmentCheck -> envCheckTasks.add(task)
                is AbilityEffect.Periodic.Particles -> particleTasks.add(task)
            }
        }

        if (potionTasks.isNotEmpty()) {
            processPotionEffects(potionTasks)
        }

        if (particleTasks.isNotEmpty()) {
            processParticles(particleTasks)
        }

        if (envCheckTasks.isNotEmpty()) {
            processEnvironmentChecks(envCheckTasks)
        }
    }

    /**
     * Process potion effects - grouped by player for single iteration.
     */
    private fun processPotionEffects(tasks: List<PeriodicTask>) {
        val byPlayer = tasks.groupBy { it.playerId }

        for ((playerId, playerTasks) in byPlayer) {
            val player = Bukkit.getPlayer(playerId) ?: continue

            val effects = playerTasks.mapNotNull { task ->
                val effect = task.effect as AbilityEffect.Periodic.ApplyPotion
                // Check if ability is still active (dependency check)
                if (!isAbilityActive(player, task.abilityKey)) null else effect.effect
            }

            if (effects.isNotEmpty()) {
                player.addPotionEffects(effects)
            }
        }
    }

    /**
     * Process particle effects - grouped by player.
     */
    private fun processParticles(tasks: List<PeriodicTask>) {
        val byPlayer = tasks.groupBy { it.playerId }

        for ((playerId, playerTasks) in byPlayer) {
            val player = Bukkit.getPlayer(playerId) ?: continue

            for (task in playerTasks) {
                val effect = task.effect as AbilityEffect.Periodic.Particles

                if (!isAbilityActive(player, task.abilityKey)) continue

                val ability = container.abilityRegistry.get(task.abilityKey) ?: continue
                val accessor = container.configLoader.getAccessor(task.abilityKey, ability.defaultOptions)

                effect.spawner.spawn(player, accessor)
            }
        }
    }

    /**
     * Process environment checks - runs async for expensive block operations.
     */
    private fun processEnvironmentChecks(tasks: List<PeriodicTask>) {
        val byPlayer = tasks.groupBy { it.playerId }

        for ((playerId, playerTasks) in byPlayer) {
            val player = Bukkit.getPlayer(playerId) ?: continue

            for (task in playerTasks) {
                val effect = task.effect as AbilityEffect.Periodic.EnvironmentCheck

                if (!isAbilityActive(player, task.abilityKey)) continue

                val ability = container.abilityRegistry.get(task.abilityKey) ?: continue
                val accessor = container.configLoader.getAccessor(task.abilityKey, ability.defaultOptions)

                // Run the check
                effect.check.check(player, accessor)
            }
        }
    }

    /**
     * Check if an ability is currently active for a player.
     * Handles dependency abilities.
     */
    private fun isAbilityActive(player: Player, abilityKey: Key): Boolean {
        val ability = container.abilityRegistry.get(abilityKey) ?: return false

        // Check dependency
        val depKey = ability.dependencyKey
        if (depKey != null) {
            val depAbility = container.abilityRegistry.getDependencyAbility(depKey)
            if (depAbility != null) {
                val isEnabled = depAbility.isEnabled(player)
                // If inverse dependency, ability is active when dependency is disabled
                if (ability.dependencyInverse) {
                    if (isEnabled) return false
                } else {
                    if (!isEnabled) return false
                }
            }
        }

        return true
    }

    /**
     * Get count of active periodic tasks.
     */
    fun getTaskCount(): Int = playerTasks.values.sumOf { it.size }

    /**
     * Get count of players with periodic tasks.
     */
    fun getPlayerCount(): Int = playerTasks.size
}
