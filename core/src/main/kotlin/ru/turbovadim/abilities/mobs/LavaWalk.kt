package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.BreakSpeedModifierAbility
import ru.turbovadim.abilities.types.FlightAllowingAbility
import ru.turbovadim.abilities.types.VisibleAbility
import kotlin.math.floor

class LavaWalk : VisibleAbility, FlightAllowingAbility, AttributeModifierAbility, BreakSpeedModifierAbility {

    override fun canFly(player: Player): Boolean {
        if (!player.isInLava) return false

        if (player.allowFlight) {
            player.isFlying = true
        }

        if (!player.isSneaking) {
            val location = player.location
            val fractionalY = location.y - floor(location.y)
            val blockAbove = location.block.getRelative(BlockFace.UP)

            if (blockAbove.type == Material.LAVA || fractionalY + 0.1 < 0.65) {
                player.teleport(location.add(0.0, 0.1, 0.0))
            } else if (0.65 - fractionalY > 0.04) {
                val newLocation = location.clone().apply {
                    y = floor(y) + 0.65
                }
                player.teleport(newLocation)
            }
        }
        return true
    }

    override val description: MutableList<LineComponent> = LineData.makeLineFor(
        "You have the ability to walk on lava source blocks! You are also quicker while walking on lava, and slower on land.",
        LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = LineData.makeLineFor("Lava Walker", LineType.TITLE)

    override val attribute: Attribute = NMSInvoker.movementSpeedAttribute

    override val amount: Double = 0.0

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER

    override fun provideContextFor(player: Player): BreakSpeedModifierAbility.BlockMiningContext {
        return BreakSpeedModifierAbility.BlockMiningContext(
            player.inventory.itemInMainHand,
            player.getPotionEffect(NMSInvoker.miningFatigueEffect),
            player.getPotionEffect(NMSInvoker.hasteEffect),
            player.getPotionEffect(PotionEffectType.CONDUIT_POWER),
            underwater = false,
            aquaAffinity = false,
            onGround = true
        )
    }

    override fun shouldActivate(player: Player): Boolean {
        return player.isInLava
    }

    override fun getFlightSpeed(player: Player): Float {
        return 0.1f
    }

    override val key: Key = Key.key("moborigins:lava_walk")
}
