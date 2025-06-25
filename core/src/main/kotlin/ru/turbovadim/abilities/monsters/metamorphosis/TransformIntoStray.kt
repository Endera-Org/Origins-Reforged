package ru.turbovadim.abilities.monsters.metamorphosis

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.AddonLoader
import ru.turbovadim.OriginSwapper
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.events.PlayerSwapOriginEvent

class TransformIntoStray : VisibleAbility, Listener {
    
    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "You transform into a Stray if you're in the cold for too long.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor("Metamorphosis", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override fun getKey(): Key = Key.key("monsterorigins:transform_into_stray")

    private val lastHadLowFreezeTime: MutableMap<Player, Int> = HashMap()

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        if (event.tickNumber % 20 != 0) return
        for (player in Bukkit.getOnlinePlayers()) {
            runForAbility(player) {
                if (player.freezeTicks < player.maxFreezeTicks) {
                    lastHadLowFreezeTime.put(player, Bukkit.getCurrentTick())
                } else if (Bukkit.getCurrentTick() - lastHadLowFreezeTime.getOrDefault(
                        player,
                        Bukkit.getCurrentTick()
                    ) >= 300
                ) {
                    MetamorphosisTemperature.Companion.setTemperature(player, 25)
                    switchToStray(player)
                } else if (MetamorphosisTemperature.Companion.getTemperature(player) <= 25) {
                    switchToStray(player)
                }
            }
        }
    }

    private fun switchToStray(player: Player) {
        player.location.getWorld()
            .playSound(player, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.PLAYERS, 1f, 1f)
        CoroutineScope(ioDispatcher).launch {
            OriginSwapper.setOrigin(
                player,
                AddonLoader.getOrigin("stray"),
                PlayerSwapOriginEvent.SwapReason.PLUGIN,
                false,
                "origin"
            )
            player.sendMessage(
                Component.text("You have transformed into a stray!")
                    .color(NamedTextColor.YELLOW)
            )
        }
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        if (event.cause == EntityDamageEvent.DamageCause.FREEZE) {
            runForAbility(event.getEntity()) { event.isCancelled = true }
        }
    }
}
