package ru.turbovadim.abilities

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.DependantAbility
import ru.turbovadim.abilities.types.VisibilityChangingAbility
import ru.turbovadim.abilities.types.VisibleAbility

class Invisibility : DependantAbility, VisibleAbility, VisibilityChangingAbility {

    override fun getKey(): Key {
        return Key.key("origins:invisibility")
    }

    override val dependencyKey: Key = Key.key("origins:phantomize")

    override val description: MutableList<LineComponent> = makeLineFor(
        "While phantomized, you are invisible.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Invisibility",
        LineComponent.LineType.TITLE
    )

    override fun isInvisible(player: Player): Boolean {
        return dependency.isEnabled(player)
    }
}
