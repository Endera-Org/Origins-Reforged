package ru.turbovadim.v2.event

import org.bukkit.entity.Player
import ru.turbovadim.v2.origin.Origin

/**
 * Reason for an origin change request.
 *
 * This is used by the internal pipeline and exposed to external hooks so addons
 * can distinguish between user-driven changes and system-driven changes.
 */
enum class OriginChangeReason {
    COMMAND,
    ORB,
    UI,
    DATABASE_LOAD,
    PLUGIN
}

/**
 * Mutable, cancellable origin change request used by the internal pipeline.
 *
 * Interceptors may:
 * - cancel the change
 * - rewrite the target layer
 * - rewrite the target origin
 */
data class OriginChangeRequest(
    val player: Player,
    var layer: String,
    val oldOrigin: Origin?,
    var newOrigin: Origin?,
    val reason: OriginChangeReason,
    var cancelled: Boolean = false
)

/**
 * Result of processing an origin change through the pipeline.
 */
data class OriginChangeResult(
    val cancelled: Boolean,
    val layer: String,
    val oldOrigin: Origin?,
    val newOrigin: Origin?,
    val changed: Boolean
)

/**
 * Internal interceptor hook for addons and plugin modules.
 */
fun interface OriginChangeInterceptor {
    fun intercept(request: OriginChangeRequest)
}

