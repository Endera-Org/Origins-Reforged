package ru.turbovadim.abilities.impossible

import net.kyori.adventure.key.Key
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.VisibleAbility

class Translucent : VisibleAbility {
    // Currently thought to be impossible without having unintended effects on 1.20.6
    // If added then add an automated notice upon an operator joining allowing them to click to either
    // disable notifications about it or to automatically add it to phantom, if the avian origin exists and is missing it
    // Auto disable these notifications if phantom is ever detected with it so the notification never shows if someone removes it
    // Starting after version 2.2.14 this ability is in the default phantom.json file however in earlier versions it will not have saved
    override fun getKey(): Key {
        return Key.key("origins:translucent")
    }

    override val description: MutableList<LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "Your skin is translucent.",
            LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<LineComponent>
        get() = OriginSwapper.LineData.makeLineFor("Translucent", LineComponent.LineType.TITLE)
}
