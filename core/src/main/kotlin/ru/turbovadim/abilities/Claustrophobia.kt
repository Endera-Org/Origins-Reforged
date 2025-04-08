package ru.turbovadim.abilities

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.jetbrains.annotations.NotNull
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginsRebornEnhanced
import ru.turbovadim.abilities.types.VisibleAbility

class Claustrophobia : VisibleAbility, Listener {
    private val stacks: MutableMap<Player, Int> = mutableMapOf()

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        if (event.tickNumber % 5 != 0) return
        Bukkit.getOnlinePlayers().forEach { player ->
            runForAbility(player) { p ->
                val currentStacks = stacks.getOrDefault(p, -200)
                val newStacks = if (p.location.block.getRelative(BlockFace.UP, 2).isSolid) {
                    minOf(currentStacks + 1, 3600)
                } else {
                    maxOf(currentStacks - 1, -200)
                }
                stacks[p] = newStacks

                if (newStacks > 0) {
                    p.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, newStacks, 0, true, true, true))
                    p.addPotionEffect(
                        PotionEffect(
                            OriginsRebornEnhanced.NMSInvoker.slownessEffect,
                            newStacks,
                            0,
                            true,
                            true,
                            true
                        )
                    )
                }
            }
        }
    }


    @EventHandler
    fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        if (event.item.type == Material.MILK_BUCKET) {
            stacks[event.player] = minOf(stacks.getOrDefault(event.player, -200), 0)
        }
    }

    @NotNull
    override fun getKey(): Key {
        return Key.key("origins:claustrophobia")
    }

    override val description: List<OriginSwapper.LineData.LineComponent> =
        makeLineFor(
            "Being somewhere with a low ceiling for too long will weaken you and make you slower.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: List<OriginSwapper.LineData.LineComponent> =
        makeLineFor(
            "Claustrophobia",
            OriginSwapper.LineData.LineComponent.LineType.TITLE
        )
}
