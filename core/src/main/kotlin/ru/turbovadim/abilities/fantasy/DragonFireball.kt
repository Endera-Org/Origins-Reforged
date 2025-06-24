package ru.turbovadim.abilities.fantasy

import com.destroystokyo.paper.MaterialTags
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.entity.DragonFireball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged
import ru.turbovadim.abilities.types.Ability.AbilityRunner
import ru.turbovadim.abilities.types.VisibleAbility

class DragonFireball : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "You can right click whilst holding a sword to launch a dragon's fireball, with a cooldown of 30 seconds.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Dragon's Fireball",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key {
        return Key.key("fantasyorigins:dragon_fireball")
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item

        runForAbility(player, AbilityRunner { _ ->
            if (!event.action.isRightClick) return@AbilityRunner
            if (event.clickedBlock != null) return@AbilityRunner
            if (item == null) return@AbilityRunner
            if (!MaterialTags.SWORDS.isTagged(item.type)) return@AbilityRunner
            if (player.getCooldown(item.type) > 0) return@AbilityRunner

            MaterialTags.SWORDS.values.forEach { player.setCooldown(it, 600) }

            val fireball = player.launchProjectile(DragonFireball::class.java)
            Bukkit.getScheduler().scheduleSyncDelayedTask(OriginsReforged.instance) {
                fireball.velocity = player.location.direction.multiply(1.2)
            }
        })
    }
}
