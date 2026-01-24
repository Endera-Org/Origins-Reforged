package ru.turbovadim.v2.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.bukkit.plugin.java.JavaPlugin
import org.endera.enderalib.utils.async.BukkitDispatcher
import ru.turbovadim.packetsenders.NMSInvoker
import ru.turbovadim.v2.ability.AbilityRegistry
import ru.turbovadim.v2.config.AbilityConfigLoader
import ru.turbovadim.v2.event.OriginEventBus
import ru.turbovadim.v2.origin.OriginLoader
import ru.turbovadim.v2.origin.OriginRegistry
import ru.turbovadim.v2.cooldown.CooldownManager
import ru.turbovadim.v2.processor.ArmorAbilityProcessor
import ru.turbovadim.v2.processor.FoodAbilityProcessor
import ru.turbovadim.v2.processor.GenericListenerProcessor
import ru.turbovadim.v2.processor.PassiveEffectProcessor
import ru.turbovadim.v2.processor.PeriodicAbilityProcessor
import ru.turbovadim.v2.processor.ReactiveAbilityProcessor
import ru.turbovadim.v2.processor.TriggeredAbilityProcessor
import ru.turbovadim.v2.state.PlayerStateManager

/**
 * Central dependency injection container for the Origins system.
 * All services are lazily initialized and accessible through this single container.
 *
 * Usage:
 * ```kotlin
 * val container = OriginsContainer.create(plugin, nmsInvoker, bukkitDispatcher)
 * val state = container.playerStateManager.getState(player)
 * ```
 */
class OriginsContainer private constructor(
    val plugin: JavaPlugin,
    val nmsInvoker: NMSInvoker,
    val dispatchers: OriginsDispatchers,
    val configLoader: AbilityConfigLoader
) {
    // Core registries
    val abilityRegistry: AbilityRegistry by lazy { AbilityRegistry(this) }
    val originRegistry: OriginRegistry by lazy { OriginRegistry(this) }

    // Origin loading
    val originLoader: OriginLoader by lazy { OriginLoader(this) }

    // State management
    val playerStateManager: PlayerStateManager by lazy { PlayerStateManager(this) }

    // Event handling
    val eventBus: OriginEventBus by lazy { OriginEventBus(this) }

    // Processors
    val passiveEffectProcessor: PassiveEffectProcessor by lazy { PassiveEffectProcessor(this) }
    val periodicAbilityProcessor: PeriodicAbilityProcessor by lazy { PeriodicAbilityProcessor(this) }
    val reactiveAbilityProcessor: ReactiveAbilityProcessor by lazy { ReactiveAbilityProcessor(this) }
    val triggeredAbilityProcessor: TriggeredAbilityProcessor by lazy { TriggeredAbilityProcessor(this) }
    val foodAbilityProcessor: FoodAbilityProcessor by lazy { FoodAbilityProcessor(this) }
    val armorAbilityProcessor: ArmorAbilityProcessor by lazy { ArmorAbilityProcessor(this) }
    val genericListenerProcessor: GenericListenerProcessor by lazy { GenericListenerProcessor(this) }

    // Cooldown management
    val cooldownManager: CooldownManager by lazy { CooldownManager() }

    /**
     * Initialize the container. Call this after all abilities and origins are registered.
     */
    fun initialize() {
        configLoader.load()
        playerStateManager.registerEvents()
        eventBus.registerEvents()
        reactiveAbilityProcessor.registerEvents()
        triggeredAbilityProcessor.registerEvents()
        foodAbilityProcessor.registerEvents()
        armorAbilityProcessor.registerEvents()
        periodicAbilityProcessor.start()
        cooldownManager.start()
    }

    /**
     * Shutdown the container. Call this on plugin disable.
     */
    fun shutdown() {
        cooldownManager.stop()
        periodicAbilityProcessor.stop()
        playerStateManager.clearAll()
    }

    companion object {
        @Volatile
        private var instance: OriginsContainer? = null

        /**
         * Create a new container instance.
         */
        fun create(
            plugin: JavaPlugin,
            nmsInvoker: NMSInvoker,
            bukkitDispatcher: BukkitDispatcher
        ): OriginsContainer {
            val dispatchers = OriginsDispatchers(
                main = bukkitDispatcher,
                io = Dispatchers.IO,
                compute = Dispatchers.Default
            )
            val configLoader = AbilityConfigLoader(plugin)

            return OriginsContainer(plugin, nmsInvoker, dispatchers, configLoader).also {
                instance = it
            }
        }

        /**
         * Get the current container instance.
         * @throws IllegalStateException if container is not initialized
         */
        fun get(): OriginsContainer {
            return instance ?: throw IllegalStateException(
                "OriginsContainer not initialized. Call create() first."
            )
        }

        /**
         * Get the current container instance or null if not initialized.
         */
        fun getOrNull(): OriginsContainer? = instance
    }
}

/**
 * Coroutine dispatchers used throughout the Origins system.
 */
data class OriginsDispatchers(
    /** Bukkit main thread dispatcher - use for Bukkit API calls */
    val main: CoroutineDispatcher,
    /** IO dispatcher - use for file operations, database calls */
    val io: CoroutineDispatcher,
    /** Compute dispatcher - use for CPU-intensive calculations */
    val compute: CoroutineDispatcher
)
