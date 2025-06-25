package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import ru.turbovadim.cooldowns.CooldownAbility
import ru.turbovadim.cooldowns.Cooldowns.CooldownInfo
import kotlin.math.max
import kotlin.math.min

class Temperature : CooldownAbility {
    override fun getKey(): Key {
        return Key.key("moborigins:temperature")
    }

    fun getTemperature(player: Player?): Int {
        return playerTemperatureMap.getOrDefault(player, 0)!!
    }

    fun setTemperature(player: Player, amount: Int) {
        playerTemperatureMap[player] = max(0, min(amount, 100))
        super.setCooldown(player, getTemperature(player))
    }

    override val cooldownInfo: CooldownInfo
        get() = CooldownInfo(100, "temperature", true, true)

    companion object {
        private val playerTemperatureMap: MutableMap<Player?, Int?> = HashMap()
        var INSTANCE: Temperature = Temperature()
    }
}
