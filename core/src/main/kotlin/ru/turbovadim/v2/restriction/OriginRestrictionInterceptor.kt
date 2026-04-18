package ru.turbovadim.v2.restriction

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import ru.turbovadim.OriginsReforged.Companion.mainConfig
import ru.turbovadim.database.DatabaseManager
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.event.OriginChangeInterceptor
import ru.turbovadim.v2.event.OriginChangeReason
import ru.turbovadim.v2.event.OriginChangeRequest

/**
 * Enforces `restrictions.reusingOrigins` and `restrictions.preventSameOrigins`:
 *
 * - `reusingOrigins = NONE` — no per-origin restrictions
 * - `reusingOrigins = PERPLAYER` — a player can't pick an origin they've held before
 * - `reusingOrigins = ALL` — no player can pick an origin any player has held before
 *                           (this implicitly also enables `preventSameOrigins`)
 * - `preventSameOrigins = true` — block picking an origin any *currently online* player has
 *
 * The interceptor skips `DATABASE_LOAD` (so players can still restore their own
 * origin on login) and `PLUGIN` (so `/origin set` administrative overrides work).
 */
class OriginRestrictionInterceptor(
    private val container: OriginsContainer
) : OriginChangeInterceptor {

    override fun intercept(request: OriginChangeRequest) {
        val newOrigin = request.newOrigin ?: return

        // Allow loads from DB and admin-driven plugin changes through.
        if (request.reason == OriginChangeReason.DATABASE_LOAD ||
            request.reason == OriginChangeReason.PLUGIN
        ) {
            return
        }

        val restrictions = mainConfig.restrictions
        val mode = restrictions.reusingOrigins.uppercase()

        // 1. reusingOrigins
        when (mode) {
            "PERPLAYER" -> {
                val used = DatabaseManager
                    .getUsedOriginsSync(request.player.uniqueId.toString())
                if (used.any { it.equals(newOrigin.name, ignoreCase = true) }) {
                    request.cancelled = true
                    request.player.sendMessage(
                        Component.text(
                            "You have already used this origin; you can't pick it again.",
                            NamedTextColor.RED
                        )
                    )
                    return
                }
            }
            "ALL" -> {
                val globallyUsed = DatabaseManager.getAllUsedOriginsSync()
                if (globallyUsed.any { it.equals(newOrigin.name, ignoreCase = true) }) {
                    request.cancelled = true
                    request.player.sendMessage(
                        Component.text(
                            "This origin has already been chosen by another player.",
                            NamedTextColor.RED
                        )
                    )
                    return
                }
            }
            // "NONE" or anything else - no reuse restriction.
            else -> {}
        }

        // 2. preventSameOrigins (locked on when reusingOrigins = ALL).
        val preventSame = restrictions.preventSameOrigins || mode == "ALL"
        if (preventSame) {
            for (online in Bukkit.getOnlinePlayers()) {
                if (online.uniqueId == request.player.uniqueId) continue
                val otherState = container.playerStateManager.getStateOrNull(online) ?: continue
                val otherOrigin = otherState.getOrigin(request.layer) ?: continue
                if (otherOrigin.name.equals(newOrigin.name, ignoreCase = true)) {
                    request.cancelled = true
                    request.player.sendMessage(
                        Component.text(
                            "Another player already has this origin.",
                            NamedTextColor.RED
                        )
                    )
                    return
                }
            }
        }
    }
}
