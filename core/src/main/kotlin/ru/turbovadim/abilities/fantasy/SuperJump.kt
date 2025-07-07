package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.VisibleAbility

class SuperJump : VisibleAbility {
    override val description: MutableList<LineComponent> = makeLineFor(
        "For years you've always felt as if your legs don't do quite enough, with all this training you can reach even higher heights.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Bouncing",
        LineComponent.LineType.TITLE
    )

    override val key: Key = Key.key("fantasyorigins:super_jump")
}
