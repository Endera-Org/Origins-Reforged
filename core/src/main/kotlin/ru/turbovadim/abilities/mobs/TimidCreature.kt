package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class TimidCreature : VisibleAbility, AttributeModifierAbility {

    override val attribute: Attribute
        get() = NMSInvoker.movementSpeedAttribute

    override val amount: Double
        get() = 0.0

    override fun getChangedAmount(player: Player): Double {
        val entities = player.getNearbyEntities(8.0, 8.0, 8.0)
        entities.removeIf { entity ->
            entity.type != EntityType.PLAYER
        }
        return if (entities.size >= 3) 0.1 else 0.0
    }

    override val operation: AttributeModifier.Operation
        get() = AttributeModifier.Operation.ADD_NUMBER

    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "Your speed increases when you are around more than 3 other players.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "Timid Creature",
            OriginSwapper.LineData.LineComponent.LineType.TITLE
        )

    override fun getKey(): Key = Key.key("moborigins:timid_creature")
}
