package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack
import ru.turbovadim.OriginSwapper
import ru.turbovadim.abilities.types.VisibleAbility

class BetterGoldWeapons : VisibleAbility, Listener {

    override val description: MutableList<OriginSwapper.LineData.LineComponent> = OriginSwapper.LineData.makeLineFor(
        "Your evil corruption of gold unlocks a dark power, making golden weapons unbreakable and much stronger.",
        OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<OriginSwapper.LineData.LineComponent> = OriginSwapper.LineData.makeLineFor(
        "Gold Desecration",
        OriginSwapper.LineData.LineComponent.LineType.TITLE
    )

    override val key: Key = Key.key("monsterorigins:better_gold_weapons")

    @EventHandler
    fun onPlayerItemDamage(event: PlayerItemDamageEvent) {
        if (event.item.type == Material.GOLDEN_SWORD || event.item.type == Material.GOLDEN_AXE) {
            runForAbility(event.getPlayer()) { event.isCancelled = true }
            
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player
        if (player != null) {
            val item: ItemStack = player.inventory.itemInMainHand
            if (item.type == Material.GOLDEN_SWORD || item.type == Material.GOLDEN_AXE) {
                runForAbility(player) { event.setDamage(event.damage * 2) }
            }
        }
    }
}
