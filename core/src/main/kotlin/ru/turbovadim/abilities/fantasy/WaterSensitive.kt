package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.VisibleAbility

class WaterSensitive : VisibleAbility {
    override val description: MutableList<LineComponent> = makeLineFor(
        "Your fiery nature makes it so that water damages you.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Water Sensitive",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key {
        return Key.key("fantasyorigins:water_sensitive")
    }
}
