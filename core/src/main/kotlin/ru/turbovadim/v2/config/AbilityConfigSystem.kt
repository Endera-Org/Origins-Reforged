package ru.turbovadim.v2.config

import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.plugin.java.JavaPlugin
import org.endera.enderalib.utils.configuration.Comment
import org.endera.enderalib.utils.configuration.ConfigurationManager
import org.endera.enderalib.utils.configuration.Spacer
import ru.turbovadim.v2.ability.AbilityConfigAccessor
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Unified configuration system for all ability options using EnderaLib.
 *
 * Uses EnderaLib's ConfigurationManager for:
 * - Type-safe config loading with kotlinx.serialization
 * - Automatic kebab-case naming in YAML
 * - Dynamic merging of old configs with new defaults
 * - @Comment annotations preserved in YAML output
 * - Error recovery with automatic backups
 */
class AbilityConfigLoader(private val plugin: JavaPlugin) {

    private val configs = ConcurrentHashMap<Key, AbilityConfigData>()
    private var configManager: ConfigurationManager<AbilitiesConfig>? = null

    /**
     * Load ability configs using EnderaLib ConfigurationManager.
     */
    fun load() {
        val configFile = File(plugin.dataFolder, "abilities.yml")

        configManager = ConfigurationManager(
            configFile = configFile,
            dataFolder = plugin.dataFolder,
            defaultConfig = defaultAbilitiesConfig,
            logger = plugin.logger,
            serializer = AbilitiesConfig.serializer(),
            clazz = AbilitiesConfig::class
        )

        // Load or create config (handles merging, backups, etc.)
        val loadedConfig = configManager!!.loadOrCreateConfig()

        // Parse into our internal format
        loadedConfig.abilities.forEach { (keyStr, data) ->
            val key = parseKey(keyStr)
            configs[key] = data
        }

        plugin.logger.info("Loaded ${configs.size} ability configurations")
    }

    /**
     * Reload configs from file.
     */
    fun reload() {
        configs.clear()
        load()
    }

    /**
     * Save current configs to file.
     */
    fun save() {
        // TODO: Implement config saving
        plugin.logger.info("Config save requested - not yet implemented")
    }

    /**
     * Get config data for an ability.
     */
    fun getConfig(key: Key): AbilityConfigData? = configs[key]

    /**
     * Get or create a config accessor for runtime access.
     */
    fun getAccessor(key: Key, defaults: Map<String, Any>): AbilityConfigAccessor {
        return AbilityConfigAccessorImpl(key, this, defaults)
    }

    /**
     * Check if an ability is visible in the UI.
     * Returns the config value if set, otherwise the default from the ability.
     */
    fun isVisible(key: Key, default: Boolean): Boolean {
        return configs[key]?.visible ?: default
    }

    /**
     * Get title override from config, or null if not set.
     */
    fun getTitle(key: Key): String? {
        return configs[key]?.title
    }

    /**
     * Get description override from config, or null if not set.
     */
    fun getDescription(key: Key): List<String>? {
        return configs[key]?.description?.takeIf { it.isNotEmpty() }
    }

    /**
     * Get attribute entries for an ability.
     */
    fun getAttributes(key: Key): List<AttributeEntry> {
        val data = configs[key] ?: return emptyList()
        return data.attributes.mapNotNull { attr ->
            try {
                AttributeEntry(
                    attribute = Attribute.valueOf(attr.attribute.uppercase().replace("-", "_")),
                    value = attr.value,
                    operation = AttributeModifier.Operation.valueOf(
                        attr.operation.uppercase().replace("-", "_")
                    )
                )
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Invalid attribute config for $key: ${attr.attribute}")
                null
            }
        }
    }

    /**
     * Register default options for an ability.
     * Merges with existing config (config file values take priority).
     */
    fun registerDefaults(key: Key, defaults: Map<String, Any>) {
        val existing = configs[key]
        if (existing == null) {
            configs[key] = AbilityConfigData(
                title = null,
                description = null,
                attributes = emptyList(),
                options = defaults.mapValues { it.value.toString() }
            )
        } else {
            // Merge: defaults first, then existing (existing wins)
            val mergedOptions = defaults.mapValues { it.value.toString() } + existing.options
            configs[key] = existing.copy(options = mergedOptions)
        }
    }

    private fun parseKey(path: String): Key {
        return if (path.contains(":")) {
            Key.key(path)
        } else {
            Key.key("origins", path)
        }
    }

    private fun keyToString(key: Key): String {
        return if (key.namespace() == "origins") {
            key.value()
        } else {
            "${key.namespace()}:${key.value()}"
        }
    }
}

// ============================================
// SERIALIZABLE CONFIG DATA CLASSES
// ============================================

/**
 * Root config structure for abilities.yml
 */
@Serializable
data class AbilitiesConfig(
    @Comment("""
        Ability configurations
        Each key is an ability name (e.g., 'climbing', 'burn-in-daylight')
        All values defined here override the defaults from code
    """)
    val abilities: Map<String, AbilityConfigData>
)

/**
 * Configuration for a single ability.
 */
@Serializable
data class AbilityConfigData(
    @Comment("Whether this ability is shown in the origin selection UI")
    val visible: Boolean? = null,

    @Spacer(1)
    @Comment("Display title (plain text, rendered with custom font)")
    val title: String? = null,

    @Comment("Description lines (plain text)")
    val description: List<String>? = null,

    @Spacer(1)
    @Comment("""
        Attribute modifiers applied by this ability
        Example:
          - attribute: generic-max-health
            value: 4.0
            operation: add-number
    """)
    val attributes: List<AttributeConfigEntry> = emptyList(),

    @Spacer(1)
    @Comment("Ability-specific options (varies per ability)")
    val options: Map<String, String> = emptyMap()
)

/**
 * Attribute modifier entry in config.
 */
@Serializable
data class AttributeConfigEntry(
    @Comment("Bukkit attribute (e.g., generic-max-health, generic-attack-damage)")
    val attribute: String,

    @Comment("Modifier value")
    val value: Double,

    @Comment("Operation: add-number, add-scalar, multiply-scalar-1")
    val operation: String = "add-number"
)

/**
 * Default empty config.
 */
val defaultAbilitiesConfig = AbilitiesConfig(
    abilities = emptyMap()
)

// ============================================
// RUNTIME TYPES
// ============================================

/**
 * Parsed attribute modifier entry.
 */
data class AttributeEntry(
    val attribute: Attribute,
    val value: Double,
    val operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER
)

/**
 * Runtime accessor for ability config values.
 */
private class AbilityConfigAccessorImpl(
    private val key: Key,
    private val loader: AbilityConfigLoader,
    private val defaults: Map<String, Any>
) : AbilityConfigAccessor {

    private val config: AbilityConfigData? get() = loader.getConfig(key)

    override fun getInt(key: String, default: Int): Int {
        return config?.options?.get(key)?.toIntOrNull()
            ?: (defaults[key] as? Number)?.toInt()
            ?: default
    }

    override fun getDouble(key: String, default: Double): Double {
        return config?.options?.get(key)?.toDoubleOrNull()
            ?: (defaults[key] as? Number)?.toDouble()
            ?: default
    }

    override fun getFloat(key: String, default: Float): Float {
        return config?.options?.get(key)?.toFloatOrNull()
            ?: (defaults[key] as? Number)?.toFloat()
            ?: default
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        return config?.options?.get(key)?.toBooleanStrictOrNull()
            ?: defaults[key] as? Boolean
            ?: default
    }

    override fun getString(key: String, default: String): String {
        return config?.options?.get(key)
            ?: defaults[key] as? String
            ?: default
    }
}
