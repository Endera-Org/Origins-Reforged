package ru.turbovadim.v2.processor

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.particle.Particle
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.endera.enderalib.utils.async.runTask
import ru.turbovadim.v2.ability.AbilityEffect
import ru.turbovadim.v2.di.OriginsContainer
import java.util.*
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

    // Tick interval -> tasks that run at the end of ticks (using ServerTickEndEvent)
    private val tickEndBuckets = ConcurrentHashMap<Int, MutableSet<PeriodicTask>>()

    // Tick counter for ServerTickEndEvent
    private val tickEndCounter = AtomicLong(0)

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
        // Register event listener for ServerTickEndEvent
        Bukkit.getPluginManager().registerEvents(this, container.plugin)
    }

    fun stop() {
        scheduledTask?.cancel()
        scheduledTask = null
        playerTasks.clear()
        tickBuckets.clear()
        tickEndBuckets.clear()
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

                // TickEnd tasks go to a separate collection (processed by ServerTickEndEvent)
                if (effect is AbilityEffect.Periodic.TickEnd) {
                    tickEndBuckets.getOrPut(effect.intervalTicks) { ConcurrentHashMap.newKeySet() }.add(task)
                } else {
                    // Add to tick bucket
                    tickBuckets.getOrPut(effect.intervalTicks) { ConcurrentHashMap.newKeySet() }.add(task)
                }
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
            if (task.effect is AbilityEffect.Periodic.TickEnd) {
                tickEndBuckets[task.effect.intervalTicks]?.remove(task)
            } else {
                tickBuckets[task.effect.intervalTicks]?.remove(task)
            }
        }
    }

    /**
     * Single tick handler - replaces 78+ individual handlers.
     *
     * Uses runAtFixedRate bukkit function under the hood
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
        val customParticleTasks = mutableListOf<PeriodicTask>()

        for (task in tasks) {
            when (task.effect) {
                is AbilityEffect.Periodic.ApplyPotion -> potionTasks.add(task)
                is AbilityEffect.Periodic.EnvironmentCheck -> envCheckTasks.add(task)
                is AbilityEffect.Periodic.Particles -> particleTasks.add(task)
                is AbilityEffect.Periodic.CustomParticles -> customParticleTasks.add(task)
                is AbilityEffect.Periodic.TickEnd -> {} // Handled by ServerTickEndEvent, not here
            }
        }

        if (potionTasks.isNotEmpty()) {
            processPotionEffects(potionTasks)
        }

        if (particleTasks.isNotEmpty()) {
            scope.launch {
                processParticlesAsync(particleTasks)
            }
        }

        if (customParticleTasks.isNotEmpty()) {
            processCustomParticles(customParticleTasks)
        }

        if (envCheckTasks.isNotEmpty()) {
            processEnvironmentChecks(envCheckTasks)
        }
    }

    /**
     * Process potion effects - grouped by player for single iteration.
     *
     * Folia: each player's mutation hops onto the player's entity scheduler so
     * it runs on the region that owns them, not the global region we tick from.
     */
    private fun processPotionEffects(tasks: List<PeriodicTask>) {
        val byPlayer = tasks.groupBy { it.playerId }

        for ((playerId, playerTasks) in byPlayer) {
            val player = Bukkit.getPlayer(playerId) ?: continue

            player.runTask(container.plugin) {
                val effects = playerTasks.mapNotNull { task ->
                    val effect = task.effect as AbilityEffect.Periodic.ApplyPotion
                    if (!isAbilityActive(player, task.abilityKey)) null else effect.effect
                }

                if (effects.isNotEmpty()) {
                    player.addPotionEffects(effects)
                }
            }
        }
    }

    /**
     * Process particle effects asynchronously using PacketEvents.
     * Sends particles to nearby players efficiently.
     */
    private fun processParticlesAsync(tasks: List<PeriodicTask>) {
        val onlinePlayers = Bukkit.getOnlinePlayers()
            .filter { it.gameMode != GameMode.SPECTATOR }
            .toList()

        val byPlayer = tasks.groupBy { it.playerId }

        for ((playerId, playerTasks) in byPlayer) {
            val player = onlinePlayers.find { it.uniqueId == playerId } ?: continue

            for (task in playerTasks) {
                val effect = task.effect as AbilityEffect.Periodic.Particles

                if (!isAbilityActive(player, task.abilityKey)) continue

                val packet = WrapperPlayServerParticle(
                    Particle(effect.particleType),
                    false,
                    Vector3d(player.location.x, player.location.y + 1.0, player.location.z),
                    Vector3f(effect.offsetX, effect.offsetY, effect.offsetZ),
                    0f,
                    effect.count
                )

                val nearbyPlayers = getNearbyPlayers(player, onlinePlayers, effect.visibilityRadius)
                val recipients = nearbyPlayers + player

                for (recipient in recipients) {
                    PacketEvents.getAPI().playerManager.sendPacket(recipient, packet)
                }
            }
        }
    }

    /**
     * Get players within range of the source player.
     */
    private fun getNearbyPlayers(
        source: Player,
        allPlayers: List<Player>,
        range: Double
    ): List<Player> {
        val location = source.location
        val rangeSquared = range * range

        return allPlayers.filter { other ->
            other != source &&
            other.world == location.world &&
            location.distanceSquared(other.location) <= rangeSquared
        }
    }

    /**
     * Process custom particle effects with spawner logic.
     *
     * Folia: spawner.spawn may touch the player / read nearby blocks, so hop
     * to the player's entity scheduler.
     */
    private fun processCustomParticles(tasks: List<PeriodicTask>) {
        val byPlayer = tasks.groupBy { it.playerId }

        for ((playerId, playerTasks) in byPlayer) {
            val player = Bukkit.getPlayer(playerId) ?: continue

            player.runTask(container.plugin) {
                for (task in playerTasks) {
                    val effect = task.effect as AbilityEffect.Periodic.CustomParticles

                    if (!isAbilityActive(player, task.abilityKey)) continue

                    val ability = container.abilityRegistry.get(task.abilityKey) ?: continue
                    val accessor = container.configLoader.getAccessor(task.abilityKey, ability.defaultOptions)

                    effect.spawner.spawn(player, accessor)
                }
            }
        }
    }

    /**
     * Process environment checks - runs per-player on the owning region thread.
     *
     * Folia: env checks read blocks around the player and may toggle flight /
     * velocity / potion effects, so they must run on the player's region.
     */
    private fun processEnvironmentChecks(tasks: List<PeriodicTask>) {
        val byPlayer = tasks.groupBy { it.playerId }

        for ((playerId, playerTasks) in byPlayer) {
            val player = Bukkit.getPlayer(playerId) ?: continue

            player.runTask(container.plugin) {
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
     * Handle ServerTickEndEvent - processes TickEnd tasks.
     * This runs AFTER player movement is processed, which is critical
     * for abilities like phasing that modify collision.
     */
    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        val tick = tickEndCounter.incrementAndGet()

        // Process each tick-end bucket that should fire this tick
        for ((interval, tasks) in tickEndBuckets) {
            if (tick % interval == 0L && tasks.isNotEmpty()) {
                processTickEndTasks(tasks.toSet())
            }
        }
    }

    /**
     * Process tick-end tasks (run at end of each server tick).
     *
     * Folia: ServerTickEndEvent fires on the global region; hop per-player
     * work onto each player's own region scheduler before touching them.
     */
    private fun processTickEndTasks(tasks: Set<PeriodicTask>) {
        val byPlayer = tasks.groupBy { it.playerId }

        for ((playerId, playerTasks) in byPlayer) {
            val player = Bukkit.getPlayer(playerId) ?: continue

            player.runTask(container.plugin) {
                for (task in playerTasks) {
                    val effect = task.effect as AbilityEffect.Periodic.TickEnd

                    if (!isAbilityActive(player, task.abilityKey)) continue

                    val ability = container.abilityRegistry.get(task.abilityKey) ?: continue
                    val accessor = container.configLoader.getAccessor(task.abilityKey, ability.defaultOptions)

                    // Run the handler
                    effect.handler.check(player, accessor)
                }
            }
        }
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
