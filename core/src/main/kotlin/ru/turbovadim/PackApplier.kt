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
            HashMap()

        // Packs from v2 addons (keyed by namespace)
        private val v2AddonPacks: MutableMap<String, OriginsReforgedResourcePackInfo> =
            HashMap()

        fun sendPacks(player: Player) {
            CoroutineScope(ioDispatcher).launch {
                // Merge both v1 and v2 addon packs
                val allPacks = HashMap<Any, OriginsReforgedResourcePackInfo>()
                allPacks.putAll(addonPacks)
                allPacks.putAll(v2AddonPacks)
                NMSInvoker.sendResourcePacks(player, getPackURL(), allPacks)
            }
        }

        fun getPackURL(): String {
            return "https://github.com/Endera-Org/Origins-Reborn-Enhanced/raw/refs/heads/master/OriginsPack.zip"
        }

        fun addResourcePack(addon: OriginsAddon, info: OriginsReforgedResourcePackInfo) {
            addonPacks.put(addon.javaClass, info)
        }

        /**
         * Add a resource pack from a v2 addon.
         * @param namespace The addon's namespace (used as key)
         * @param info The resource pack info wrapper
         */
        fun addResourcePackV2(namespace: String, info: OriginsReforgedResourcePackInfo) {
            v2AddonPacks[namespace] = info
        }
    }
}
