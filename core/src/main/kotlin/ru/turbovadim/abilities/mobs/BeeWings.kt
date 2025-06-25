package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.cooldowns.CooldownAbility
import ru.turbovadim.cooldowns.Cooldowns.CooldownInfo

class BeeWings : VisibleAbility, Listener, CooldownAbility {
    override val description: MutableList<LineComponent>
        get() = LineData.makeLineFor(
            "You can use your tiny bee wings to descend slower as an ability.",
            LineType.DESCRIPTION
        )

    override val title: MutableList<LineComponent>
        get() = LineData.makeLineFor("Bee Wings", LineType.TITLE)

    override fun getKey(): Key {
        return Key.key("moborigins:bee_wings")
    }

    private val lastToggledSneak: MutableMap<Player, Int> = HashMap()

    @EventHandler
    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
        if (!event.isSneaking) return
        val player = event.player
        runForAbility(player) {
            if (hasCooldown(player)) return@runForAbility

            val currentTick = Bukkit.getCurrentTick()
            val lastTick = lastToggledSneak.getOrDefault(player, currentTick - 11)
            if (currentTick - lastTick <= 10) {
                setCooldown(player)
                player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, false, true))
            } else {
                lastToggledSneak[player] = currentTick
            }
        }
    }

    override val cooldownInfo: CooldownInfo
        get() = CooldownInfo(300, "bee_wings")
}
