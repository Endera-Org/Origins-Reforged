package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.abilities.types.VisibleAbility

class Stinger : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = LineData.makeLineFor(
        "When you punch someone with your fist, you poison them for a few seconds.",
        LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = LineData.makeLineFor("Stinger", LineType.TITLE)

    override val key: Key = Key.key("moborigins:stinger")

    private val lastStungTicks: MutableMap<Player, Int> = HashMap()

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val entity = event.entity as? LivingEntity ?: return
        val player = event.damager as? Player ?: return
        runForAbility(player) {
            val currentTick = Bukkit.getCurrentTick()
            val lastStungTick = lastStungTicks.getOrDefault(player, currentTick - 100)
            if (currentTick - lastStungTick >= 100) {
                lastStungTicks[player] = currentTick
                entity.addPotionEffect(PotionEffect(PotionEffectType.POISON, 60, 0, false, true))
            }
        }
    }
}
