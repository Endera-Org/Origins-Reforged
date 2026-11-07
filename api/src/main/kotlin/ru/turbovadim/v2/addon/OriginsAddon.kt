package ru.turbovadim.v2.addon

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.turbovadim.v2.ability.Ability
import ru.turbovadim.v2.ability.AbilityCheckResult
import ru.turbovadim.v2.api.OriginsApi
import ru.turbovadim.v2.origin.Origin

/**
 * Base class for Origins addons.
 *
 * Addons can provide:
 * - Custom abilities
 * - Custom origins
 * - Ability check hooks
 * - Resource packs
 *
 * Example:
 * ```kotlin
 * class MyAddon : OriginsAddon() {
 *     override val namespace = "myaddon"
 *
 *     override fun abilities() = listOf(
 *         ability("super_jump") {
 *             title = text("Super Jump")
 *             description("Jump really high!")
 *
 *             option("jump_boost", 2.0)
 *
 *             onJump { player, config ->
 *                 val boost = config.getDouble("jump_boost", 2.0)
 *                 player.velocity = player.velocity.setY(boost)
 *             }
 *         }
 *     )
 *
 *     override fun origins() = listOf(
 *         origin("bouncer") {
 *             displayName = text("Bouncer")
 *             description = listOf(text("Jump around!"))
 *             ability("super_jump")
 *         }
 *     )
 * }
 * ```
 */
abstract class OriginsAddon : JavaPlugin() {

    /**
     * Namespace for this addon's abilities and origins.
     * Default is the plugin name in lowercase.
     */
    open val namespace: String
        get() = name.lowercase().replace(" ", "_")

    /**
     * The API facade is injected during onEnable.
     * Use this to interact with the Origins system.
     */
    protected lateinit var api: OriginsApi
        private set

    /**
     * Define abilities provided by this addon.
     * Override to provide custom abilities.
     */
    protected open fun abilities(): List<Ability> = emptyList()

    /**
     * Define origins provided by this addon.
     * Override to provide custom origins.
     */
    protected open fun origins(): List<Origin> = emptyList()

    /**
     * Hook for custom ability availability checks.
     * Return null to use default behavior, or a result to override.
     */
    protected open fun onAbilityCheck(player: Player, ability: Key): AbilityCheckResult? = null

    /**
     * Resource pack info for this addon.
     * Override to provide a resource pack.
     */
    protected open fun resourcePack(): ResourcePackInfo? = null

    /**
     * Called after all abilities and origins are registered.
     * Override for custom initialization logic.
     */
    protected open fun onAddonEnable() {}

    /**
     * Called before the addon is disabled.
     * Override for cleanup logic.
     */
    protected open fun onAddonDisable() {}

    // Lifecycle methods - do not override

    final override fun onEnable() {
        // Get the API facade
        api = OriginsApi.get()

        // Register abilities
        val abilities = abilities()
        abilities.forEach { ability ->
            api.registerAbility(ability)
        }

        // Register origins
        val origins = origins()
        origins.forEach { origin ->
            api.registerOrigin(origin)
        }

        // Register ability check hook if overridden
        val hook = AbilityCheckHook { player, ability -> onAbilityCheck(player, ability) }
        api.registerAbilityCheckHook(hook)

        // Register resource pack
        resourcePack()?.let { pack ->
            api.registerResourcePack(namespace, pack)
        }

        logger.info("Registered ${abilities.size} abilities and ${origins.size} origins")

        onAddonEnable()
    }

    final override fun onDisable() {
        onAddonDisable()
    }
}
