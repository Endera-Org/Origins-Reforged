package ru.turbovadim.v2.api

import org.bukkit.plugin.java.JavaPlugin

/**
 * Service-loaded registration module for built-in content packs.
 *
 * Implementations are discovered by core via [java.util.ServiceLoader] and
 * should register abilities/origins through [OriginsApi].
 */
interface BuiltinRegistrationModule {
    /** Unique module id (typically namespace). */
    val id: String

    /** Register this module's content via public API. */
    fun register(api: OriginsApi, plugin: JavaPlugin)
}
