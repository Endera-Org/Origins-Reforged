package ru.turbovadim.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import ru.turbovadim.abilities.types.DependencyAbility

class PlaceholderDependencyAbility : DependencyAbility {
    override fun getKey(): Key {
        return Key.key("origins:blank_dependency")
    }

    override fun isEnabled(player: Player): Boolean {
        return false
    }
}
