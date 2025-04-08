package ru.turbovadim.abilities

import ru.turbovadim.AddonLoader.getTextFor
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent

interface VisibleAbility : Ability {
    val description: List<LineComponent>
    val title: List<LineComponent>

    val usedDescription: List<LineComponent>
        get() {
            val keyText = getKey().toString().replace(":", ".")
            val text = getTextFor("power.$keyText.description")
            return if (text != null) {
                makeLineFor(text, LineComponent.LineType.DESCRIPTION)
            } else {
                description
            }
        }

    val usedTitle: List<LineComponent>
        get() {
            val keyText = getKey().toString().replace(":", ".")
            val text = getTextFor("power.$keyText.name")
            return if (text != null) {
                makeLineFor(text, LineComponent.LineType.TITLE)
            } else {
                title
            }
        }
}
