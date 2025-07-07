package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.cooldowns.CooldownAbility
import ru.turbovadim.cooldowns.Cooldowns.CooldownInfo
import ru.turbovadim.events.PlayerLeftClickEvent

class PotionAction : VisibleAbility, Listener, CooldownAbility {
    override val description: MutableList<LineComponent>
        get() = LineData.makeLineFor(
            "Get a random potion effect, based on the situation you are in.",
            LineType.DESCRIPTION
        )

    override val title: MutableList<LineComponent> = LineData.makeLineFor("Perfect Potion", LineType.TITLE)

    override val key: Key = Key.key("moborigins:potion_action")

    @EventHandler
    fun onPlayerLeftClick(event: PlayerLeftClickEvent) {
        if (event.item != null) return

        val player = event.player
        runForAbility(player) {
            if (hasCooldown(player)) return@runForAbility
            setCooldown(player)

            player.world.playSound(player, Sound.ENTITY_WITCH_DRINK, SoundCategory.VOICE, 1f, 1f)

            val effectType = when {
                player.fireTicks > 0 -> PotionEffectType.FIRE_RESISTANCE
                player.fallDistance >= 4 -> PotionEffectType.SLOW_FALLING
                NMSInvoker.isUnderWater(player) -> PotionEffectType.WATER_BREATHING
                else -> PotionEffectType.SPEED
            }

            player.addPotionEffect(PotionEffect(effectType, 200, 0))
        }
    }

    override val cooldownInfo: CooldownInfo
        get() = CooldownInfo(600, "potion_action")
}
