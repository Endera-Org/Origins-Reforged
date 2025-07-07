package ru.turbovadim.cooldowns

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import ru.turbovadim.OriginsReforged
import ru.turbovadim.OriginsReforged.Companion.getCooldowns
import ru.turbovadim.OriginsReforged.Companion.instance
import ru.turbovadim.abilities.types.Ability
import ru.turbovadim.cooldowns.Cooldowns.CooldownInfo

@JvmDefaultWithCompatibility
@Suppress("unused") // Some functions here are unused but are useful in addons
interface CooldownAbility : Ability {
    val cooldownKey: NamespacedKey
        get() = NamespacedKey(instance, key.asString().replace(":", "-"))

    fun setCooldown(player: Player) {
        if (OriginsReforged.mainConfig.cooldowns.disableAllCooldowns) return
        getCooldowns().setCooldown(player, cooldownKey)
    }

    fun setCooldown(player: Player, amount: Int) {
        if (OriginsReforged.mainConfig.cooldowns.disableAllCooldowns) return
        getCooldowns().setCooldown(player, cooldownKey, amount, cooldownInfo.isStatic)
    }

    fun hasCooldown(player: Player): Boolean {
        if (OriginsReforged.mainConfig.cooldowns.disableAllCooldowns) return false
        return getCooldowns().hasCooldown(player, cooldownKey)
    }

    fun getCooldown(player: Player): Long {
        return getCooldowns().getCooldown(player, cooldownKey)
    }

    val cooldownInfo: CooldownInfo
}
