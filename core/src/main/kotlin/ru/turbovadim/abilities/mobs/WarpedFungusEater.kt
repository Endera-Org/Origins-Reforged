package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginSwapper
import ru.turbovadim.abilities.types.VisibleAbility
import kotlin.math.min

class WarpedFungusEater : VisibleAbility, Listener {

    override val description: MutableList<OriginSwapper.LineData.LineComponent> = OriginSwapper.LineData.makeLineFor(
        "You can eat warped fungus to recover some hunger, along with a small speed boost.",
        OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<OriginSwapper.LineData.LineComponent> =
        OriginSwapper.LineData.makeLineFor("Fungus Hunger", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override val key: Key = Key.key("moborigus:warped_fungus_eater")

    private val lastInteractedTicks: MutableMap<Player, Int> = HashMap()

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        runForAbility(event.getPlayer()) {
            if (lastInteractedTicks.getOrDefault(event.getPlayer(), -1) == Bukkit.getCurrentTick()) return@runForAbility
            lastInteractedTicks.put(event.getPlayer(), Bukkit.getCurrentTick())
            if (event.getAction().isRightClick) {
                if (event.getItem() == null) return@runForAbility
                if (event.getItem()!!.type == Material.WARPED_FUNGUS) {
                    if (event.hand != null) {
                        if (event.hand == EquipmentSlot.OFF_HAND) event.getPlayer().swingOffHand()
                        else event.getPlayer().swingMainHand()
                    }
                    event.getItem()!!.amount = event.getItem()!!.amount - 1
                    event.getPlayer().addPotionEffect(PotionEffect(PotionEffectType.SPEED, 200, 0, false, true))
                    event.getPlayer().foodLevel = min(event.getPlayer().foodLevel + 1, 20)
                    event.getPlayer().saturation = min(
                        event.getPlayer().saturation + 1,
                        event.getPlayer().foodLevel.toFloat()
                    )
                }
            }
        }
    }
}
