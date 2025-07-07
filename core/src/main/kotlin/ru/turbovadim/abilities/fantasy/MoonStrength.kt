package ru.turbovadim.abilities.fantasy

import io.papermc.paper.world.MoonPhase
import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class MoonStrength : VisibleAbility, AttributeModifierAbility {
    override val description: MutableList<LineComponent> = makeLineFor(
        "You're a worshipper of the moon, so on nights with a full moon you're stronger than normal.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Moon's Blessing",
        LineComponent.LineType.TITLE
    )

    override val key: Key = Key.key("fantasyorigins:moon_strength")

    override val attribute: Attribute = NMSInvoker.attackDamageAttribute
    override val amount: Double = 0.0
    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1

    override fun getChangedAmount(player: Player): Double {
        if (!player.world.isDayTime && player.world.moonPhase == MoonPhase.FULL_MOON) {
            return 2.4
        }
        return 0.0
    }
}
