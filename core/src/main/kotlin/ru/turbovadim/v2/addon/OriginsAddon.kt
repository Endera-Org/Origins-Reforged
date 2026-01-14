package ru.turbovadim.v2.addon

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.turbovadim.v2.ability.Ability
import ru.turbovadim.v2.ability.AbilityCheckResult
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.origin.Origin

/**
 * Base class for Origins addons (v2 API).
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
     * The container is injected during onEnable.
     * Use this to access registries and services.
     */
    protected lateinit var container: OriginsContainer
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
        // Get or wait for container
        container = OriginsContainer.get()

        // Register abilities
        val abilities = abilities()
        abilities.forEach { ability ->
            container.abilityRegistry.register(ability)
            container.configLoader.registerDefaults(ability.key, ability.defaultOptions)
        }

        // Register origins
        val origins = origins()
        origins.forEach { origin ->
            container.originRegistry.register(origin)
        }

        // Register ability check hook if overridden
        // TODO: Add hook registration to container

        // Register resource pack
        resourcePack()?.let { pack ->
            // TODO: Register with PackApplier
        }

        logger.info("Registered ${abilities.size} abilities and ${origins.size} origins")

        onAddonEnable()
    }

    final override fun onDisable() {
        onAddonDisable()
    }
}

/**
 * Information about a resource pack provided by an addon.
 */
data class ResourcePackInfo(
    /** URL to the resource pack */
    val url: String,
    /** SHA-1 hash of the resource pack */
    val hash: String,
    /** Whether the pack is required */
    val required: Boolean = false,
    /** Prompt shown to the player */
    val prompt: String? = null
)

/**
 * Registry for addon-provided ability check hooks.
 */
class AddonAbilityCheckRegistry {
    private val hooks = mutableListOf<AbilityCheckHook>()

    fun register(hook: AbilityCheckHook) {
        hooks.add(hook)
    }

    fun unregister(hook: AbilityCheckHook) {
        hooks.remove(hook)
    }

    /**
     * Check all hooks and return first non-null result.
     */
    fun check(player: Player, ability: Key): AbilityCheckResult? {
        for (hook in hooks) {
            val result = hook.check(player, ability)
            if (result != null) return result
        }
        return null
    }
}

/**
 * Hook interface for addon-provided ability checks.
 */
fun interface AbilityCheckHook {
    fun check(player: Player, ability: Key): AbilityCheckResult?
}
