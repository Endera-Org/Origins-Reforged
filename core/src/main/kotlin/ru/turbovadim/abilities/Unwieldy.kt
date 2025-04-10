package ru.turbovadim.abilities

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.InteractionHand
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetCooldown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.VisibleAbility


class Unwieldy : VisibleAbility, Listener, PacketListener {

    val packet = WrapperPlayServerSetCooldown(ItemTypes.SHIELD, 1000)

    override fun onPacketReceive(event: PacketReceiveEvent) {
        if (event.packetType != PacketType.Play.Client.USE_ITEM) return
        val player: Player = event.getPlayer()

        runForAbility(player) {
            val useItemPacket = WrapperPlayClientUseItem(event)

            val item = if (useItemPacket.hand == InteractionHand.MAIN_HAND) {
                it.inventory.itemInMainHand
            } else {
                it.inventory.itemInOffHand
            }

            if (isShield(item)) {
                event.isCancelled = true
            }
        }

    }

    private fun isShield(item: ItemStack): Boolean {
        return item.type == Material.SHIELD
    }

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        CoroutineScope(ioDispatcher).launch {
            Bukkit.getOnlinePlayers().toList().forEach { player ->
                runForAbility(player) {
                    PacketEvents.getAPI().playerManager.sendPacket(player, packet)
//                    player.setCooldown(Material.SHIELD, 1000)
                }
            }
        }
    }


    override fun getKey(): Key {
        return Key.key("origins:no_shield")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "The way your hands are formed provide no way of holding a shield upright.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor("Unwieldy", LineComponent.LineType.TITLE)
}
