package ru.turbovadim.v2.ability

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import ru.turbovadim.v2.api.OriginsApi
import kotlin.reflect.KClass

/**
 * Type-safe state key for storing per-player ability state.
 *
 * Usage in ability DSL:
 * ```kotlin
 * val myAbility = ability("my_ability") {
 *     val counter = intState("counter", default = 0)
 *     val enabled = boolState("enabled", default = false)
 *
 *     onTick(interval = 20) { player, _ ->
 *         val current = counter[player]
 *         counter[player] = current + 1
 *     }
 * }
 * ```
 *
 * @param T The type of the state value
 * @param abilityKey The ability this state belongs to
 * @param name The name of this state variable
 * @param default The default value when state is not set
 * @param type The KClass for runtime type checking
 */
class StateKey<T : Any>(
    val abilityKey: Key,
    val name: String,
    val default: T,
    val type: KClass<T>
) {
    /**
     * Composite key used for storage: "namespace:ability:stateName"
     */
    val storageKey: String = "${abilityKey.asString()}:$name"

    /**
     * Get the state value for a player.
     * Returns the default if not set.
     */
    operator fun get(player: Player): T {
        return OriginsApi.get().getState(player, this)
    }

    /**
     * Set the state value for a player.
     */
    operator fun set(player: Player, value: T) {
        OriginsApi.get().setState(player, this, value)
    }

    /**
     * Reset the state to its default value for a player.
     */
    fun reset(player: Player) {
        OriginsApi.get().resetState(player, this)
    }

    /**
     * Update the state value using a transform function.
     * Returns the new value.
     */
    inline fun update(player: Player, transform: (T) -> T): T {
        val current = get(player)
        val new = transform(current)
        set(player, new)
        return new
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StateKey<*>) return false
        return storageKey == other.storageKey
    }

    override fun hashCode(): Int = storageKey.hashCode()

    override fun toString(): String = "StateKey($storageKey, default=$default)"
}
