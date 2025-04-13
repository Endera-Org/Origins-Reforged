package ru.turbovadim.util

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.flags.StringFlag
import com.sk89q.worldguard.protection.regions.RegionContainer
import org.bukkit.Location
import ru.turbovadim.abilities.types.Ability

object WorldGuardHook {
    private var rc: RegionContainer? = null

    fun isAbilityDisabled(location: Location, ability: Ability): Boolean {
        try {
            val manager = rc!!.get(BukkitAdapter.adapt(location.getWorld()))
            if (manager == null) return false
            val set = manager.getApplicableRegions(
                BlockVector3(
                    location.blockX,
                    location.blockY,
                    location.blockZ
                )
            )
            for (r in set) {
                val data = r.getFlag<StringFlag?, String?>(flag)
                if (data != null) {
                    for (s in data.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                        if (s == ability.getKey().toString()) return true
                    }
                }
            }
            return false
        } catch (t: Throwable) {
            return false
        }
    }

    private var flag: StringFlag? = null

    fun tryInitialize(): Boolean {
        val registry = WorldGuard.getInstance().flagRegistry

        flag = StringFlag("origins-reborn:disabled-abilities")
        registry.register(flag)

        return true
    }

    fun completeInitialize() {
        rc = WorldGuard.getInstance().platform.regionContainer
    }
}
