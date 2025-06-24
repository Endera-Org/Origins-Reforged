package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.Ability.AbilityRunner
import ru.turbovadim.abilities.types.VisibleAbility

class Chime : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "You can absorb the chime of amethyst shards to regenerate health.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Chime",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key = Key.key("fantasyorigins:chime")

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (!event.action.isRightClick) return
        val item = event.item ?: return
        if (item.type != Material.AMETHYST_SHARD) return

        val player = event.player
        runForAbility(player) { _ ->
            item.amount--
            player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 900, 1))
            event.hand?.let { hand ->
                when (hand) {
                    EquipmentSlot.HAND -> player.swingMainHand()
                    else -> player.swingOffHand()
                }
            }
        }
    }
}
