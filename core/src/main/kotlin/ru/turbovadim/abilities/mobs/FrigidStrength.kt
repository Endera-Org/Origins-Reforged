package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class FrigidStrength : VisibleAbility, AttributeModifierAbility {
    override val attribute: Attribute
        get() = NMSInvoker.attackDamageAttribute

    override val amount: Double = 0.0

    override fun getChangedAmount(player: Player): Double {
        return if (player.location.block.temperature < 0.15) 3.0 else 0.0
    }

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER

    override val description: MutableList<LineComponent>
        get() = LineData.makeLineFor("Deal more damage in cold areas.", LineType.DESCRIPTION)

    override val title: MutableList<LineComponent>
        get() = LineData.makeLineFor("Frigid Strength", LineType.TITLE)

    override val key: Key = Key.key("moborigins:frigid_strength")
}
