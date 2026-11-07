package ru.turbovadim.v2.addon

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import ru.turbovadim.v2.ability.AbilityCheckResult

/**
 * Registry for addon-provided ability check hooks.
 */
class AddonAbilityCheckRegistry {
    private val hooks = mutableListOf<AbilityCheckHook>()

    fun register(hook: AbilityCheckHook) {
        hooks.add(hook)
    }

    fun unregister(hook: AbilityCheckHook) {
        hooks.remove(hook)
    }

    /**
     * Check all hooks and return first non-null result.
     */
    fun check(player: Player, ability: Key): AbilityCheckResult? {
        for (hook in hooks) {
            val result = hook.check(player, ability)
            if (result != null) return result
        }
        return null
    }
}
