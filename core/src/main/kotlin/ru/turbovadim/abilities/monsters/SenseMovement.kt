package ru.turbovadim.abilities.monsters

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility

class SenseMovement : VisibleAbility, Listener {
    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "You can see the outlines of nearby mobs, even through blocks.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "Heightened Senses",
            OriginSwapper.LineData.LineComponent.LineType.TITLE
        )

    override val key: Key
        get() {
            return Key.key("monsterorigins:sense_movement")
        }

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        if (event.tickNumber % 20 != 0) return
        for (player in Bukkit.getOnlinePlayers()) {
            runForAbility(player) {
                for (entity in player.getNearbyEntities(24.0, 24.0, 24.0)) {
                    if (entity === player) continue
                    if (entity !is LivingEntity) continue

                    var data: Byte = 0
                    if (entity.isGlowing || entity.location.distance(player.location) <= 16) {
                        data = (data + 0x40).toByte()
                    }
                    if (entity.fireTicks > 0) {
                        data = (data + 0x01).toByte()
                    }
                    if (entity.isInvisible) {
                        data = (data + 0x20).toByte()
                    }
                    if (entity is Player) {
                        if (entity.isSneaking) {
                            data = (data + 0x02).toByte()
                        }
                        if (entity.isSprinting) {
                            data = (data + 0x08).toByte()
                        }
                        if (entity.isSwimming) {
                            data = (data + 0x10).toByte()
                        }
                        if (entity.isGliding) {
                            data = (data + 0x80.toByte()).toByte()
                        }
                    }

                    NMSInvoker.sendEntityData(player, entity, data)
                }
            }
        }
    }
}
