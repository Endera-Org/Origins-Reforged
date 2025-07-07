package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.Ability.AbilityRunner
import ru.turbovadim.abilities.types.VisibleAbility
import kotlin.math.min

class Leeching : VisibleAbility, Listener {
    override val key: Key = Key.key("fantasyorigins:leeching")

    override val description: MutableList<LineComponent> = makeLineFor(
        "Upon killing a mob or player, you sap a portion of its health, healing you.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Leeching",
        LineComponent.LineType.TITLE
    )

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return
        runForAbility(killer, AbilityRunner { player ->
            val killerMaxHealth = player.getAttribute(NMSInvoker.maxHealthAttribute)?.value ?: return@AbilityRunner
            val mobMaxHealth = event.entity.getAttribute(NMSInvoker.maxHealthAttribute)?.value ?: return@AbilityRunner
            player.health = min(killerMaxHealth, player.health + (mobMaxHealth / 5))
        })
    }
}
