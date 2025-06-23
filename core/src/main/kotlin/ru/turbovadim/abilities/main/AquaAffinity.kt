package ru.turbovadim.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsRebornEnhanced.Companion.NMSInvoker
import ru.turbovadim.abilities.types.BreakSpeedModifierAbility
import ru.turbovadim.abilities.types.BreakSpeedModifierAbility.BlockMiningContext
import ru.turbovadim.abilities.types.VisibleAbility

class AquaAffinity : VisibleAbility, BreakSpeedModifierAbility {
    override fun getKey(): Key {
        return Key.key("origins:aqua_affinity")
    }

    override val description: MutableList<LineComponent> = makeLineFor("You may break blocks underwater as others do on land.", LineComponent.LineType.DESCRIPTION)

    override val title: MutableList<LineComponent> = makeLineFor("Aqua Affinity", LineComponent.LineType.TITLE)

    override fun provideContextFor(player: Player) = with(player) {
        BlockMiningContext(
            inventory.itemInMainHand,
            getPotionEffect(NMSInvoker.hasteEffect),
            getPotionEffect(NMSInvoker.miningFatigueEffect),
            getPotionEffect(PotionEffectType.CONDUIT_POWER),
            true,
            true,
            true
        )
    }

    override fun shouldActivate(player: Player): Boolean {
        return player.isInWater
    }
}
