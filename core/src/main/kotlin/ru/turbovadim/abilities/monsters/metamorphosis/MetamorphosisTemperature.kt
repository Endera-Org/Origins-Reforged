package ru.turbovadim.abilities.monsters.metamorphosis

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.persistence.PersistentDataType
import ru.turbovadim.OriginsReforged
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.cooldowns.CooldownAbility
import ru.turbovadim.cooldowns.Cooldowns
import ru.turbovadim.events.PlayerSwapOriginEvent
import kotlin.math.max
import kotlin.math.min

class MetamorphosisTemperature : CooldownAbility, Listener {

    override val key: Key = Key.key("monsterorigins:metamorphosis_temperature")

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        if (event.tickNumber % 20 != 0) return
        for (player in Bukkit.getOnlinePlayers()) {
            runForAbility(player) {
                val blockTemp = player.location.block.temperature
                if (blockTemp <= 0.15) {
                    setTemperature(player, getTemperature(player) - 1)
                } else if (blockTemp >= 1.75 && !NMSInvoker.isUnderWater(player)) {
                    setTemperature(player, getTemperature(player) + 1)
                }
            }
        }
    }

    @EventHandler
    fun onPlayerSwapOrigin(event: PlayerSwapOriginEvent) {
        if (listOf(
                PlayerSwapOriginEvent.SwapReason.ORB_OF_ORIGIN,
                PlayerSwapOriginEvent.SwapReason.DIED,
                PlayerSwapOriginEvent.SwapReason.COMMAND,
                PlayerSwapOriginEvent.SwapReason.INITIAL
            ).contains(event.reason)
        ) setTemperature(event.getPlayer(), 50)
    }

    override val cooldownInfo: Cooldowns.CooldownInfo = Cooldowns.CooldownInfo(100, "metamorphosis_temperature", true, true)

    companion object {
        private val playerTemperatureKey: NamespacedKey =
            NamespacedKey(OriginsReforged.instance, "player-temperature")

        var INSTANCE: MetamorphosisTemperature = MetamorphosisTemperature()

        fun getTemperature(player: Player): Int {
            return player.persistentDataContainer
                .getOrDefault(playerTemperatureKey, PersistentDataType.INTEGER, 50)
        }

        fun setTemperature(player: Player, amount: Int) {
            player.persistentDataContainer
                .set(playerTemperatureKey, PersistentDataType.INTEGER, max(0, min(amount, 100)))
            INSTANCE.setCooldown(player, getTemperature(player))
        }
    }
}
