package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerToggleSneakEvent
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.cooldowns.CooldownAbility
import ru.turbovadim.cooldowns.Cooldowns

class Explosive : VisibleAbility, Listener, CooldownAbility {

    override val key: Key
        get() = Key.key("monsterorigins:explosive")

    override val description: MutableList<LineComponent> = makeLineFor(
        "You can sacrifice some of your health to create an explosion every 15 seconds.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Explosive",
        LineComponent.LineType.TITLE
    )

    private val lastToggledSneak: MutableMap<Player, Int> = mutableMapOf()

    @EventHandler
    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
        val player = event.player
        runForAbility(player) {
            if (hasCooldown(player)) return@runForAbility
            if (!event.isSneaking) return@runForAbility
            val lastTick = lastToggledSneak.getOrDefault(player, Bukkit.getCurrentTick() - 11)
            if (Bukkit.getCurrentTick() - lastTick <= 10) {
                setCooldown(player)
                player.location.createExplosion(
                    player,
                    3f,
                    false,
                    true
                )
                NMSInvoker.dealExplosionDamage(player, 8)
            } else {
                lastToggledSneak[player] = Bukkit.getCurrentTick()
            }
        }
    }

    override val cooldownInfo: Cooldowns.CooldownInfo = Cooldowns.CooldownInfo(300, "explosive")
}
