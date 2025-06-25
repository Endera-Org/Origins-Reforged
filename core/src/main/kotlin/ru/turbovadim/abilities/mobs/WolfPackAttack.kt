package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility

class WolfPackAttack : AttributeModifierAbility {

    override fun getChangedAmount(player: Player): Double {
        val entities = player.getNearbyEntities(8.0, 8.0, 8.0)
        entities.removeIf { entity ->
            entity!!.type != EntityType.WOLF
        }
        return (if (entities.size >= 4) 2 else 0).toDouble()
    }

    override val attribute: Attribute
        get() = NMSInvoker.attackDamageAttribute

    override val amount: Double
        get() = 0.0

    override val operation: AttributeModifier.Operation
        get() = AttributeModifier.Operation.ADD_NUMBER

    override fun getKey(): Key = Key.key("moborigins:wolf_pack_attack")
}
