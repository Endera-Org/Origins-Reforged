package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.VisibleAbility

class DaylightSensitive : VisibleAbility {
    override val description: MutableList<LineComponent> = makeLineFor(
        "Your dark nature makes it so that you become less powerful during daylight.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Daylight Sensitivity",
        LineComponent.LineType.TITLE
    )

    override val key: Key = Key.key("fantasyorigins:daylight_sensitive")
}
