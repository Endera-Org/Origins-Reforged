package ru.turbovadim.v2.config

import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.plugin.java.JavaPlugin
import org.endera.enderalib.utils.configuration.Comment
import org.endera.enderalib.utils.configuration.ConfigurationManager
import org.endera.enderalib.utils.configuration.Spacer
import ru.turbovadim.v2.ability.Ability
import ru.turbovadim.v2.ability.AbilityConfigAccessor
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Folder-based configuration system for abilities.
 *
 * Abilities are organized by namespace in subfolders:
 * ```
 * abilities/
 * ├── origins/
 * │   ├── climbing.yml
 * │   ├── tailwind.yml
 * │   └── ...
 * ├── fantasyorigins/
 * │   ├── double_health.yml
 * │   └── ...
 * └── moborigins/
 *     ├── bee_wings.yml
 *     └── ...
 * ```
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
    private val abilitiesFolder: File by lazy {
        File(plugin.dataFolder, "abilities").also { it.mkdirs() }
    }

    /**
     * Load all ability configs from the abilities folder.
     * Scans all namespace subfolders for YAML files.
     */
    fun load() {
        configs.clear()

        // Scan namespace subfolders
        abilitiesFolder.listFiles { file -> file.isDirectory }?.forEach { namespaceFolder ->
            val namespace = namespaceFolder.name

            // Load all YAML files in this namespace folder
            namespaceFolder.listFiles { file -> file.extension == "yml" }?.forEach { file ->
                try {
                    loadAbilityConfig(namespace, file)
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to load ability config ${namespace}/${file.name}: ${e.message}")
                }
            }
        }

        plugin.logger.info("Loaded ${configs.size} ability configurations from abilities/ folder")
    }

    /**
     * Load a single ability config file from a namespace folder.
     */
    private fun loadAbilityConfig(namespace: String, file: File) {
        val abilityName = file.nameWithoutExtension
        val key = Key.key(namespace, abilityName)
        val namespaceFolder = file.parentFile

        val configManager = ConfigurationManager(
            configFile = file,
            dataFolder = namespaceFolder,
            defaultConfig = AbilityConfigData(),
            logger = plugin.logger,
            serializer = AbilityConfigData.serializer(),
            clazz = AbilityConfigData::class
        )

        val data = configManager.loadOrCreateConfig()
        configs[key] = data
    }

    /**
     * Reload configs from files.
     */
    fun reload() {
        load()
    }

    /**
     * Save config for a specific ability by regenerating the file.
     * The file is created/updated using ConfigurationManager's merge behavior.
     */
    fun save(key: Key) {
        val data = configs[key] ?: return
        val namespaceFolder = getNamespaceFolder(key.namespace())
        val file = File(namespaceFolder, "${key.value()}.yml")

        val configManager = ConfigurationManager(
            configFile = file,
            dataFolder = namespaceFolder,
            defaultConfig = data,
            logger = plugin.logger,
            serializer = AbilityConfigData.serializer(),
            clazz = AbilityConfigData::class
        )

        // loadOrCreateConfig writes the file if missing and merges with defaults
        configManager.loadOrCreateConfig()
    }

    /**
     * Save all ability configs.
     */
    fun saveAll() {
        configs.keys.forEach { save(it) }
        plugin.logger.info("Saved ${configs.size} ability configurations")
    }

    /**
     * Generate default config file for an ability if it doesn't exist.
     * Called during ability registration.
     */
    fun generateDefaultConfig(ability: Ability) {
        val namespaceFolder = getNamespaceFolder(ability.key.namespace())
        val file = File(namespaceFolder, "${ability.key.value()}.yml")

        // Build default config from ability
        val defaultData = AbilityConfigData(
            visible = ability.isVisibleDefault,
            title = PlainTextComponentSerializer.plainText().serialize(ability.title),
            description = ability.description.map {
                PlainTextComponentSerializer.plainText().serialize(it)
            }.takeIf { it.isNotEmpty() },
            options = ability.defaultOptions.mapValues { it.value.toString() }
        )

        val configManager = ConfigurationManager(
            configFile = file,
            dataFolder = namespaceFolder,
            defaultConfig = defaultData,
            logger = plugin.logger,
            serializer = AbilityConfigData.serializer(),
            clazz = AbilityConfigData::class
        )

        // Load or create - this handles merging existing config with new defaults
        val loadedConfig = configManager.loadOrCreateConfig()
        configs[ability.key] = loadedConfig
    }

    /**
     * Get or create the folder for a namespace.
     */
    private fun getNamespaceFolder(namespace: String): File {
        return File(abilitiesFolder, namespace).also { it.mkdirs() }
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
     * Register default options for an ability.
     * Merges with existing config (config file values take priority).
     */
    fun registerDefaults(key: Key, defaults: Map<String, Any>) {
        val existing = configs[key]
        if (existing == null) {
            configs[key] = AbilityConfigData(
                title = null,
                description = null,
                options = defaults.mapValues { it.value.toString() }
            )
        } else {
            // Merge: defaults first, then existing (existing wins)
            val mergedOptions = defaults.mapValues { it.value.toString() } + existing.options
            configs[key] = existing.copy(options = mergedOptions)
        }
    }
}

// ============================================
// SERIALIZABLE CONFIG DATA CLASSES
// ============================================

/**
 * Configuration for a single ability.
 * Each ability has its own YAML file with this structure.
 */
@Serializable
data class AbilityConfigData(
    @Comment("Whether this ability is shown in the origin selection UI")
    val visible: Boolean? = null,

    @Spacer(1)
    @Comment("Display title (plain text)")
    val title: String? = null,

    @Comment("Description lines (plain text)")
    val description: List<String>? = null,

    @Spacer(1)
    @Comment("Ability-specific options (varies per ability)")
    val options: Map<String, String> = emptyMap()
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
