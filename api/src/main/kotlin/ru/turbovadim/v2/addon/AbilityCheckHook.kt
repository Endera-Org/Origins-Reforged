package ru.turbovadim.v2.addon

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import ru.turbovadim.v2.ability.AbilityCheckResult

/**
 * Hook interface for addon-provided ability checks.
 */
fun interface AbilityCheckHook {
    fun check(player: Player, ability: Key): AbilityCheckResult?
}
