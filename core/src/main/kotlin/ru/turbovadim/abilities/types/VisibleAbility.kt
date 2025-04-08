package ru.turbovadim.abilities.types

import ru.turbovadim.OriginSwapper.LineData.LineComponent

interface VisibleAbility : Ability {
    val description: List<LineComponent>
    val title: List<LineComponent>
}
