package ru.turbovadim.abilities.mobs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.AddonLoader
import ru.turbovadim.OriginSwapper
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.events.PlayerSwapOriginEvent.SwapReason

class BecomesElderGuardian : VisibleAbility, Listener {

    override val description: MutableList<OriginSwapper.LineData.LineComponent> = OriginSwapper.LineData.makeLineFor(
        "Defeating an Elder Guardian will turn you into one!",
        OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<OriginSwapper.LineData.LineComponent> = OriginSwapper.LineData.makeLineFor(
        "Become Elder Guardian",
        OriginSwapper.LineData.LineComponent.LineType.TITLE
    )

    override val key: Key = Key.key("moborigins:becomes_elder_guardian")

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        if (entity.type != EntityType.ELDER_GUARDIAN) return
        val player = entity.killer ?: return
        CoroutineScope(ioDispatcher).launch {
            runForAbilityAsync(player) {
                OriginSwapper.setOrigin(
                    player,
                    AddonLoader.getOrigin("elder guardian"),
                    SwapReason.PLUGIN,
                    false,
                    AddonLoader.layers[0]!!
                )
                player.sendMessage(
                    Component.text("You have grown into an Elder Guardian!")
                        .color(NamedTextColor.YELLOW)
                )
            }
        }
    }
}
