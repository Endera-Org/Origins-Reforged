package ru.turbovadim.v2.processor

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.particle.Particle
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.endera.enderalib.utils.async.EntityScheduler
import ru.turbovadim.v2.util.refreshPotionEffect
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
 * - Player work stays on the owning region
 */
class PeriodicAbilityProcessor(private val container: OriginsContainer) : Listener {


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

        processBatch(tickBuckets.entries.filter { tick % it.key == 0L }.flatMap { it.value }.toSet())
    }

    /**
     * Process a batch of tasks efficiently.
     */
    private fun processBatch(tasks: Set<PeriodicTask>) {
        for ((playerId, due) in tasks.groupBy { it.playerId }) {
            val player = Bukkit.getPlayer(playerId) ?: continue
            EntityScheduler.execute(container.plugin, player, {
                if (!player.isOnline) return@execute
                var flightRefreshed = false
                var visibilityRefreshed = false
                for (task in due) {
                    if (task !in playerTasks[playerId].orEmpty() || !isAbilityActive(player, task.abilityKey)) continue
                    val ability = container.abilityRegistry.get(task.abilityKey) ?: continue
                    val config = container.configLoader.getAccessor(task.abilityKey, ability.defaultOptions)
                    when (val effect = task.effect) {
                        is AbilityEffect.Passive.Flight -> if (!flightRefreshed) {
                            container.passiveEffectProcessor.refreshFlight(player)
                            flightRefreshed = true
                        }
                        is AbilityEffect.Passive.Invisibility -> if (!visibilityRefreshed) {
                            container.passiveEffectProcessor.refreshVisibility(player)
                            visibilityRefreshed = true
                        }
                        is AbilityEffect.Periodic.ApplyPotion -> player.refreshPotionEffect(effect.effect)
                        is AbilityEffect.Periodic.EnvironmentCheck -> effect.check.check(player, config)
                        is AbilityEffect.Periodic.CustomParticles -> effect.spawner.spawn(player, config)
                        is AbilityEffect.Periodic.TickEnd -> effect.handler.check(player, config)
                        is AbilityEffect.Periodic.Particles -> {
                            val location = player.location
                            val packet = WrapperPlayServerParticle(
                                Particle(effect.particleType), false,
                                Vector3d(location.x, location.y + 1.0, location.z),
                                Vector3f(effect.offsetX, effect.offsetY, effect.offsetZ), 0f, effect.count
                            )
                            // Tracking is maintained by the server, avoiding a scan of every online player.
                            (player.trackedBy + player).forEach { viewer ->
                                EntityScheduler.execute(container.plugin, viewer, {
                                    val target = viewer.location
                                    if (target.world == location.world && target.distanceSquared(location) <= effect.visibilityRadius * effect.visibilityRadius) {
                                        PacketEvents.getAPI().playerManager.sendPacket(viewer, packet)
                                    }
                                })
                            }
                        }
                    }
                }
            })
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

        processBatch(tickEndBuckets.entries.filter { tick % it.key == 0L }.flatMap { it.value }.toSet())
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
