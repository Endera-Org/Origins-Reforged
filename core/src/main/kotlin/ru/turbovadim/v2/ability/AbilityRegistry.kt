package ru.turbovadim.v2.ability

import net.kyori.adventure.key.Key
import ru.turbovadim.v2.di.OriginsContainer

/**
 * Registry for all registered abilities.
 *
 * Provides O(1) lookups and maintains pre-computed indexes
 * for efficient querying of abilities by effect type.
 */
class AbilityRegistry(private val container: OriginsContainer) {

    // Primary storage
    private val abilities = mutableMapOf<Key, Ability>()

    // Indexes by effect type for O(1) lookups
    private val flightAbilities = mutableSetOf<Key>()
    private val invisibilityAbilities = mutableSetOf<Key>()
    private val periodicAbilities = mutableMapOf<Key, List<AbilityEffect.Periodic>>()
    private val reactiveAbilities = mutableMapOf<Key, List<AbilityEffect.Reactive>>()
    private val triggeredAbilities = mutableMapOf<Key, List<AbilityEffect.Triggered>>()
    private val listenerAbilities = mutableMapOf<Key, List<AbilityEffect.Listener>>()

    // Dependency abilities for fast lookup
    private val dependencyAbilities = mutableMapOf<Key, DependencyAbility>()

    // Multi-ability mapping (sub-ability key -> parent multi-ability)
    private val multiAbilityMap = mutableMapOf<Key, MutableList<MultiAbility>>()

    /**
     * Register an ability.
     */
    fun register(ability: Ability) {
        abilities[ability.key] = ability

        // Index by effect type
        indexEffects(ability)

        // Handle special ability types
        if (ability is DependencyAbility) {
            dependencyAbilities[ability.key] = ability
        }

        if (ability is MultiAbility) {
            ability.subAbilities.forEach { sub ->
                multiAbilityMap.getOrPut(sub.key) { mutableListOf() }.add(ability)
            }
        }

        // Register listener effects with the processor
        val listeners = ability.effects.filterIsInstance<AbilityEffect.Listener>()
        if (listeners.isNotEmpty()) {
            listenerAbilities[ability.key] = listeners
            container.genericListenerProcessor.registerAbility(ability)
        }
    }

    /**
     * Unregister an ability.
     */
    fun unregister(key: Key): Ability? {
        val ability = abilities.remove(key) ?: return null

        // Remove from indexes
        flightAbilities.remove(key)
        invisibilityAbilities.remove(key)
        periodicAbilities.remove(key)
        reactiveAbilities.remove(key)
        triggeredAbilities.remove(key)
        dependencyAbilities.remove(key)

        // Remove listener effects
        if (listenerAbilities.remove(key) != null) {
            container.genericListenerProcessor.unregisterAbility(key)
        }

        // Remove multi-ability mappings
        if (ability is MultiAbility) {
            ability.subAbilities.forEach { sub ->
                multiAbilityMap[sub.key]?.remove(ability)
            }
        }

        return ability
    }

    /**
     * Get ability by key.
     */
    fun get(key: Key): Ability? = abilities[key]

    /**
     * Get all registered abilities.
     */
    fun getAll(): Collection<Ability> = abilities.values

    /**
     * Get all ability keys that grant flight.
     */
    fun getFlightAbilities(): Set<Key> = flightAbilities.toSet()

    /**
     * Get all ability keys that grant invisibility.
     */
    fun getInvisibilityAbilities(): Set<Key> = invisibilityAbilities.toSet()

    /**
     * Get periodic effects for an ability.
     */
    fun getPeriodicEffects(key: Key): List<AbilityEffect.Periodic> {
        return periodicAbilities[key] ?: emptyList()
    }

    /**
     * Get reactive effects for an ability.
     */
    fun getReactiveEffects(key: Key): List<AbilityEffect.Reactive> {
        return reactiveAbilities[key] ?: emptyList()
    }

    /**
     * Get triggered effects for an ability.
     */
    fun getTriggeredEffects(key: Key): List<AbilityEffect.Triggered> {
        return triggeredAbilities[key] ?: emptyList()
    }

    /**
     * Get a dependency ability by key.
     */
    fun getDependencyAbility(key: Key): DependencyAbility? = dependencyAbilities[key]

    /**
     * Get parent multi-abilities for a sub-ability.
     */
    fun getMultiAbilitiesContaining(subKey: Key): List<MultiAbility> {
        return multiAbilityMap[subKey] ?: emptyList()
    }

    /**
     * Get the flight effect for an ability (if it has one).
     */
    fun getFlightEffect(key: Key): AbilityEffect.Passive.Flight? {
        val ability = abilities[key] ?: return null
        return ability.effects.filterIsInstance<AbilityEffect.Passive.Flight>().firstOrNull()
    }

    /**
     * Get the invisibility effect for an ability (if it has one).
     */
    fun getInvisibilityEffect(key: Key): AbilityEffect.Passive.Invisibility? {
        val ability = abilities[key] ?: return null
        return ability.effects.filterIsInstance<AbilityEffect.Passive.Invisibility>().firstOrNull()
    }

    /**
     * Check if an ability key is registered.
     */
    fun contains(key: Key): Boolean = abilities.containsKey(key)

    /**
     * Get count of registered abilities.
     */
    val size: Int get() = abilities.size

    /**
     * Clear all registered abilities.
     */
    fun clear() {
        abilities.clear()
        flightAbilities.clear()
        invisibilityAbilities.clear()
        periodicAbilities.clear()
        reactiveAbilities.clear()
        triggeredAbilities.clear()
        listenerAbilities.clear()
        dependencyAbilities.clear()
        multiAbilityMap.clear()
    }

    // Private helpers

    private fun indexEffects(ability: Ability) {
        val periodic = mutableListOf<AbilityEffect.Periodic>()
        val reactive = mutableListOf<AbilityEffect.Reactive>()
        val triggered = mutableListOf<AbilityEffect.Triggered>()

        for (effect in ability.effects) {
            when (effect) {
                is AbilityEffect.Passive.Flight -> {
                    flightAbilities.add(ability.key)
                }
                is AbilityEffect.Passive.Invisibility -> {
                    invisibilityAbilities.add(ability.key)
                }
                is AbilityEffect.Periodic -> {
                    periodic.add(effect)
                }
                is AbilityEffect.Reactive -> {
                    reactive.add(effect)
                }
                is AbilityEffect.Triggered -> {
                    triggered.add(effect)
                }
                is AbilityEffect.Listener -> {
                    // Handled separately in register()
                }
            }
        }

        if (periodic.isNotEmpty()) {
            periodicAbilities[ability.key] = periodic
        }
        if (reactive.isNotEmpty()) {
            reactiveAbilities[ability.key] = reactive
        }
        if (triggered.isNotEmpty()) {
            triggeredAbilities[ability.key] = triggered
        }
    }
}
