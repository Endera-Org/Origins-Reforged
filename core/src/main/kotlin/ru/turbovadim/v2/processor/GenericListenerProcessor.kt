package ru.turbovadim.v2.processor

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.Listener
import ru.turbovadim.v2.ability.Ability
import ru.turbovadim.v2.ability.AbilityEffect
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.event.OriginChangedEvent
import kotlin.reflect.KClass

/**
 * Processor for generic event listener effects.
 * Dynamically registers Bukkit listeners based on ability definitions.
 */
class GenericListenerProcessor(private val container: OriginsContainer) {

    private data class HandlerEntry<E : Event>(
        val abilityKey: Key,
        val effect: AbilityEffect.Listener.Generic<E>
    )

    private data class OriginChangedHandlerEntry(
        val abilityKey: Key,
        val effect: AbilityEffect.Listener.OriginChanged
    )

    // eventClass -> list of handler entries
    private val handlers = mutableMapOf<KClass<*>, MutableList<HandlerEntry<*>>>()
    private val registeredEvents = mutableSetOf<KClass<*>>()
    private val originChangedHandlers = mutableListOf<OriginChangedHandlerEntry>()
    private var originChangedRegistered = false

    /**
     * Register an ability's listener effects.
     * Called when an ability is registered with the AbilityRegistry.
     */
    fun registerAbility(ability: Ability) {
        val listenerEffects = ability.effects.filterIsInstance<AbilityEffect.Listener>()
        for (effect in listenerEffects) {
            when (effect) {
                is AbilityEffect.Listener.Generic<*> -> {
                    addHandler(ability.key, effect)
                    ensureEventRegistered(effect)
                }
                is AbilityEffect.Listener.OriginChanged -> {
                    addOriginChangedHandler(ability.key, effect)
                    ensureOriginChangedRegistered()
                }
            }
        }
    }

    /**
     * Unregister an ability's listener effects.
     */
    fun unregisterAbility(abilityKey: Key) {
        handlers.values.forEach { entries ->
            entries.removeAll { it.abilityKey == abilityKey }
        }
        originChangedHandlers.removeAll { it.abilityKey == abilityKey }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <E : Event> addHandler(abilityKey: Key, effect: AbilityEffect.Listener.Generic<E>) {
        val entries = handlers.getOrPut(effect.eventClass) { mutableListOf() }
        entries.add(HandlerEntry(abilityKey, effect))
    }

    private fun ensureEventRegistered(effect: AbilityEffect.Listener.Generic<*>) {
        if (effect.eventClass in registeredEvents) return
        registeredEvents += effect.eventClass

        @Suppress("UNCHECKED_CAST")
        val eventClass = effect.eventClass.java as Class<out Event>

        Bukkit.getPluginManager().registerEvent(
            eventClass,
            object : Listener {},
            effect.priority,
            { _, event -> dispatchEvent(event) },
            container.plugin,
            effect.ignoreCancelled
        )
    }

    private fun addOriginChangedHandler(abilityKey: Key, effect: AbilityEffect.Listener.OriginChanged) {
        originChangedHandlers.add(OriginChangedHandlerEntry(abilityKey, effect))
    }

    private fun ensureOriginChangedRegistered() {
        if (originChangedRegistered) return
        originChangedRegistered = true

        container.eventBus.registerChangedListener { event ->
            dispatchOriginChanged(event)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun dispatchEvent(event: Event) {
        val entries = handlers[event::class] ?: return

        for (entry in entries) {
            val typedEntry = entry as HandlerEntry<Event>
            val player = typedEntry.effect.playerExtractor(event) ?: continue

            // Check if player has this ability
            val state = container.playerStateManager.getState(player)
            if (!state.hasAbility(entry.abilityKey)) continue

            // Check if ability is active (dependency check)
            if (!isAbilityActive(player, entry.abilityKey)) continue

            // Get config accessor
            val ability = container.abilityRegistry.get(entry.abilityKey) ?: continue
            val accessor = container.configLoader.getAccessor(entry.abilityKey, ability.defaultOptions)

            // Call the handler
            typedEntry.effect.handler(player, event, accessor)
        }
    }

    private fun dispatchOriginChanged(event: OriginChangedEvent) {
        if (originChangedHandlers.isEmpty()) return
        val player = event.player
        val state = container.playerStateManager.getState(player)

        for (entry in originChangedHandlers) {
            if (!state.hasAbility(entry.abilityKey)) continue
            if (!isAbilityActive(player, entry.abilityKey)) continue

            val ability = container.abilityRegistry.get(entry.abilityKey) ?: continue
            val accessor = container.configLoader.getAccessor(entry.abilityKey, ability.defaultOptions)

            entry.effect.handler(player, event, accessor)
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
}
