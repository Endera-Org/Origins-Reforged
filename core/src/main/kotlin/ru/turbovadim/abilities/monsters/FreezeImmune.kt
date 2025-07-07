package ru.turbovadim.abilities.monsters

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import ru.turbovadim.abilities.types.Ability

class FreezeImmune : Ability, Listener {
    override val key: Key
        get() {
            return Key.key("monsterorigins:freeze_immune")
        }

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent?) {
        for (player in Bukkit.getOnlinePlayers()) {
            runForAbility(player) { player.freezeTicks = 0 }
        }
    }
}
