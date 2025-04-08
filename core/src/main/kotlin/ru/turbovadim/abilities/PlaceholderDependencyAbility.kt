package ru.turbovadim.abilities

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player

class PlaceholderDependencyAbility : DependencyAbility {
    override fun getKey(): Key {
        return Key.key("origins:blank_dependency")
    }

    override fun isEnabled(player: Player): Boolean {
        return false
    }
}
