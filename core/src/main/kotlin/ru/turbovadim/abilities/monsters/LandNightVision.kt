package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.main.CatVision

class LandNightVision : CatVision() {
    override val key: Key = Key.key("monsterorigins:land_night_vision")

    override val description: MutableList<LineComponent> = makeLineFor(
        "You can see in the dark when on land.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor("Dark Sight", LineComponent.LineType.TITLE)
}
