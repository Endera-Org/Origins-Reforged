package ru.turbovadim.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsRebornEnhanced.Companion.instance

class FlightToggleCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender is Player) {
            if (canFly(sender)) {
                setCanFly(sender, false)
                sender.sendMessage(Component.text("Disabled flight").color(NamedTextColor.AQUA))
            } else {
                setCanFly(sender, true)
                sender.sendMessage(Component.text("Enabled flight").color(NamedTextColor.AQUA))
            }
        } else sender.sendMessage(Component.text("This command can only be used by players!").color(NamedTextColor.RED))
        return true
    }

    init {
        key = NamespacedKey(instance, "flight")
    }

    companion object {
        @JvmStatic
        fun canFly(player: Player): Boolean {
            /*
        World w = BukkitAdapter.adapt(player.getWorld());
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(w);
        if (manager != null) {
            for (ProtectedRegion r : manager.getApplicableRegions(new BlockVector3(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ()))) {
                r.getFlags()
            }
        }

         */
            return player.persistentDataContainer.has<Byte, Boolean>(key, OriginSwapper.BooleanPDT.BOOLEAN)
        }

        @JvmStatic
        fun setCanFly(player: Player, b: Boolean) {
            if (b) player.persistentDataContainer.set<Byte, Boolean>(key, OriginSwapper.BooleanPDT.BOOLEAN, true)
            else player.persistentDataContainer.remove(key)
        }

        private lateinit var key: NamespacedKey
    }
}
