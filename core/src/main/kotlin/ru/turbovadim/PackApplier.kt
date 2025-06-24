package ru.turbovadim

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.packetsenders.OriginsReforgedResourcePackInfo

class PackApplier : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (OriginsReforged.mainConfig.resourcePack.enabled) {
            if (ShortcutUtils.isBedrockPlayer(event.getPlayer().uniqueId)) return
            sendPacks(event.getPlayer())
        }
    }

    companion object {
        private val addonPacks: MutableMap<Class<out OriginsAddon>, OriginsReforgedResourcePackInfo> =
            HashMap<Class<out OriginsAddon>, OriginsReforgedResourcePackInfo>()

        fun sendPacks(player: Player) {
            CoroutineScope(ioDispatcher).launch {
                NMSInvoker.sendResourcePacks(player, getPackURL(), addonPacks)
            }
        }

        fun getPackURL(): String {
            return "https://github.com/Endera-Org/Origins-Reborn-Enhanced/raw/refs/heads/master/OriginsPack.zip"
        }

        fun addResourcePack(addon: OriginsAddon, info: OriginsReforgedResourcePackInfo) {
            addonPacks.put(addon.javaClass, info)
        }
    }
}
