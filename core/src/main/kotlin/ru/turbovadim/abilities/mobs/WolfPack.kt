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

class WolfPack : VisibleAbility, AttributeModifierAbility {
    
    override val attribute: Attribute
        get() = NMSInvoker.maxHealthAttribute

    override val amount: Double
        get() = 0.0

    override fun getChangedAmount(player: Player): Double {
        val entities = player.getNearbyEntities(8.0, 8.0, 8.0)
        entities.removeIf { entity ->
            entity!!.type != EntityType.WOLF
        }
        return if (entities.size >= 4) 0.04 else 0.0
    }

    override val operation: AttributeModifier.Operation
        get() = AttributeModifier.Operation.ADD_NUMBER

    override fun getKey(): Key = Key.key("moborigins:wolf_pack")

    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "When you are near at least 4 wolves you gain speed and attack damage.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor("Wolf Pack", OriginSwapper.LineData.LineComponent.LineType.TITLE)
}
