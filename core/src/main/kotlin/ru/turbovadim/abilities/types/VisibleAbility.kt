package ru.turbovadim.abilities.types

import ru.turbovadim.OriginSwapper.LineData.LineComponent

interface VisibleAbility : Ability {
    val description: MutableList<LineComponent>
    val title: List<LineComponent>
}
