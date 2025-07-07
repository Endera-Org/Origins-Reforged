package ru.turbovadim.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.Ability
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.MultiAbility
import ru.turbovadim.abilities.types.VisibleAbility

class ExtraReach : VisibleAbility, MultiAbility {
    override val key: Key = Key.key("origins:extra_reach")

    override val description: MutableList<LineComponent> = makeLineFor(
        "You can reach blocks and entities further away.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Slender Body",
        LineComponent.LineType.TITLE
    )

    override val abilities: MutableList<Ability> = listOf(
        ExtraReachBlocks.Companion.extraReachBlocks,
        ExtraReachEntities.Companion.extraReachEntities
    ).toMutableList()

    class ExtraReachEntities : AttributeModifierAbility {

        override val attribute: Attribute = NMSInvoker.entityInteractionRangeAttribute!!

        override val amount: Double = 1.5

        override val operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER

        override val key: Key
            get() {
                return Key.key("origins:extra_reach_entities")
            }

        companion object {
            var extraReachEntities: ExtraReachEntities = ExtraReachEntities()
        }
    }

    class ExtraReachBlocks : AttributeModifierAbility {

        override val attribute: Attribute = NMSInvoker.blockInteractionRangeAttribute!!

        override val amount: Double = 1.5

        override val operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER

        override val key: Key
            get() {
                return Key.key("origins:extra_reach_blocks")
            }

        companion object {
            var extraReachBlocks: ExtraReachBlocks = ExtraReachBlocks()
        }
    }
}
