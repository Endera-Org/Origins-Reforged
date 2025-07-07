package ru.turbovadim.abilities.mobs

import io.papermc.paper.world.MoonPhase
import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility

class FullMoonAttack : AttributeModifierAbility {
    override val attribute: Attribute
        get() = NMSInvoker.attackDamageAttribute

    override val amount: Double = 0.0

    override fun getChangedAmount(player: Player): Double {
        return (if (!player.world.isDayTime && player.world.moonPhase == MoonPhase.FULL_MOON) 2 else 0).toDouble()
    }

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER

    override val key: Key = Key.key("moborigins:full_moon_attack")
}
