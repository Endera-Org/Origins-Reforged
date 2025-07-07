package ru.turbovadim.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.Ability

class DamageFromPotions : Ability, Listener {
    override val key: Key = Key.key("origins:damage_from_potions")

    @EventHandler
    fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        if (event.item.type != Material.POTION) return
        runForAbility(event.player) { player ->
            NMSInvoker.dealFreezeDamage(player, 2)
        }
    }

}
