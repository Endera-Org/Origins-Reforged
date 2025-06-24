package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.VisibleAbility

class IncreasedSpeed : VisibleAbility {
    override val description: MutableList<LineComponent> = makeLineFor(
        "From years of training for race after race, you're much faster than any normal horse.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Dashmaster",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key {
        return Key.key("fantasyorigins:increased_speed")
    }
}
