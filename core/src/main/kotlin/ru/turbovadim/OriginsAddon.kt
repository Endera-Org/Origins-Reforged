package ru.turbovadim

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.turbovadim.abilities.AbilityRegister
import ru.turbovadim.abilities.custom.ToggleableAbility
import ru.turbovadim.abilities.types.Ability
import ru.turbovadim.events.PlayerSwapOriginEvent
import ru.turbovadim.packetsenders.OriginsReforgedResourcePackInfo
import java.io.File

abstract class OriginsAddon : JavaPlugin() {

    companion object {
        private lateinit var instance: OriginsAddon
        fun getInstance(): OriginsAddon = instance
    }

    open fun shouldOpenSwapMenu(): SwapStateGetter? = null

    open fun shouldAllowOriginSwapCommand(): SwapStateGetter? = null

    open fun getAbilityOverride(): KeyStateGetter? = null

    interface SwapStateGetter {
        fun get(player: Player, reason: PlayerSwapOriginEvent.SwapReason): State
    }

    interface KeyStateGetter {
        fun get(player: Player, key: Key): State
    }

    @Suppress("unused")
    enum class State {
        ALLOW,
        DEFAULT,
        DENY
    }

    final override fun onEnable() {
        instance = this
        onOnEnable()
        onRegister()
        AddonLoader.register(this)
        for (ability in getAbilities()) {
            if (ability is ToggleableAbility && !ability.shouldRegister()) continue
            AbilityRegister.registerAbility(ability, this)
        }
        getResourcePackInfo()?.let { PackApplier.addResourcePack(this, it) }
        afterRegister()
    }

    open fun onOnEnable() {}

    open fun getResourcePackInfo(): OriginsReforgedResourcePackInfo? = null

    public override fun getFile(): File = super.getFile()

    open fun onRegister() {}

    open fun afterRegister() {}

    abstract fun getNamespace(): String

    open fun getAbilities(): List<Ability> = listOf()
}
