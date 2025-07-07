package ru.turbovadim.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import ru.turbovadim.abilities.types.DependencyAbility

class PlaceholderDependencyAbility : DependencyAbility {
    override val key: Key = Key.key("origins:blank_dependency")

    override fun isEnabled(player: Player): Boolean {
        return false
    }
}
