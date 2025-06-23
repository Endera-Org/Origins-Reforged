package ru.turbovadim.abilities.main

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import ru.turbovadim.AddonLoader.getTextFor
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsRebornEnhanced
import ru.turbovadim.abilities.types.Ability.AbilityRunner
import ru.turbovadim.abilities.types.VisibleAbility

class FreshAir : VisibleAbility, Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val clickedBlock = event.clickedBlock ?: return
        if (!event.action.isRightClick) return

        val player = event.player
        val inventory = player.inventory
        if (player.isSneaking &&
            inventory.itemInOffHand.type == Material.AIR &&
            inventory.itemInMainHand.type == Material.AIR
        ) return

        if (!Tag.BEDS.isTagged(clickedBlock.type)) return

        runForAbility(player, AbilityRunner { p ->
            if (clickedBlock.y >= 86) return@AbilityRunner

            val overworld = OriginsRebornEnhanced.mainConfig.worlds.world

            val world = Bukkit.getWorld(overworld) ?: return@AbilityRunner
            if (p.world != world) return@AbilityRunner

            val blockWorld = clickedBlock.world
            if (blockWorld.isDayTime && blockWorld.isClearWeather) return@AbilityRunner

            event.isCancelled = true
            p.swingMainHand()
            p.sendActionBar(
                Component.text(
                    getTextFor("origins.avian_sleep_fail", "You need fresh air to sleep")
                )
            )
        })
    }


    override fun getKey(): Key {
        return Key.key("origins:fresh_air")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "When sleeping, your bed needs to be at an altitude of at least ${OriginsRebornEnhanced.mainConfig.extraSettings.freshAirRequiredSleepHeight} blocks, so you can breathe fresh air.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Fresh Air",
        LineComponent.LineType.TITLE
    )
}
