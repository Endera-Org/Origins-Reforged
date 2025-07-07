package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.event.Listener
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility

class SurfaceSlowness : AttributeModifierAbility, Listener {
    override val key: Key = Key.key("moborigins:surface_slowness")

    override val attribute: Attribute = NMSInvoker.movementSpeedAttribute

    override val amount: Double = -0.4

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1
}
