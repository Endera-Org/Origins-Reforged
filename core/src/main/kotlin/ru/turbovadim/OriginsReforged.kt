package ru.turbovadim

import com.github.retrooper.packetevents.PacketEvents
import com.noxcrew.interfaces.InterfacesListeners
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.endera.enderalib.bstats.MetricsLite
import org.endera.enderalib.utils.async.BukkitDispatcher
import org.endera.enderalib.utils.configuration.ConfigurationManager
import org.endera.enderalib.utils.configuration.MultiConfigurationManager
import ru.turbovadim.commands.OriginCommand
import ru.turbovadim.config.*
import ru.turbovadim.database.initDb
import ru.turbovadim.packetsenders.*
import ru.turbovadim.v2.BuiltinModuleBootstrap
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.listener.OriginCommandDispatcher
import ru.turbovadim.v2.listener.OriginDeathListener
import ru.turbovadim.v2.listener.OriginJoinFlowListener
import ru.turbovadim.v2.listener.OriginSelectionInvulnerabilityListener
import ru.turbovadim.v2.listener.OriginUsageTracker
import ru.turbovadim.v2.restriction.OriginRestrictionInterceptor
import ru.turbovadim.v2.ui.ShulkerInventoryUI
import java.io.File

class OriginsReforged : JavaPlugin() {

    companion object {

        lateinit var instance: OriginsReforged
            private set

        lateinit var multiConfigurationManager: MultiConfigurationManager
        lateinit var bukkitDispatcher: BukkitDispatcher

        lateinit var mainConfig: MainConfig
        lateinit var charactersConfig: CharactersConfig

        lateinit var NMSInvoker: NMSInvoker
            private set

        private fun initializeNMSInvoker(instance: OriginsReforged) {
            val version = Bukkit.getMinecraftVersion()
            NMSInvoker = when (version) {
                "1.21.1" -> NMSInvokerV1_21_1()
                "1.21.2", "1.21.3" -> NMSInvokerV1_21_3()
                "1.21.4" -> NMSInvokerV1_21_4()
                "1.21.5", "1.21.6" -> NMSInvokerV1_21_6()
                "1.21.7", "1.21.8" -> NMSInvokerV1_21_7()
                "1.21.9", "1.21.10" -> NMSInvokerV1_21_10()
                "1.21.11" -> NMSInvokerV1_21_11()
                "26.1.1", "26.1.2" -> loadNMSInvoker("ru.turbovadim.packetsenders.NMSInvokerV26_1")
                else -> throw IllegalStateException("Unsupported version: " + Bukkit.getMinecraftVersion())
            }
            Bukkit.getPluginManager().registerEvents(NMSInvoker, instance)
        }

        private fun loadNMSInvoker(className: String): NMSInvoker {
            val clazz = Class.forName(className, true, OriginsReforged::class.java.classLoader)
            return clazz.getDeclaredConstructor().newInstance() as NMSInvoker
        }

        /** v2 container - initialized in onEnable() */
        var v2Container: OriginsContainer? = null
            private set
    }

    var economy: Economy? = null
        private set

    private fun setupEconomy(): Boolean {
        try {
            val economyProvider = server.servicesManager.getRegistration(Economy::class.java)
            if (economyProvider != null) {
                economy = economyProvider.getProvider()
            }
            return (economy != null)
        } catch (_: NoClassDefFoundError) {
            return false
        }
    }

    var isVaultEnabled: Boolean = false
        private set

    public override fun getFile(): File = super.getFile()

    override fun onLoad() {
        instance = this
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().load()
    }

    override fun onDisable() {
        OrbRecipe.unregister()
        v2Container?.shutdown()
        PacketEvents.getAPI().terminate()
    }

    override fun onEnable() {

        bukkitDispatcher = BukkitDispatcher(this)
        InterfacesListeners.install(this)
        initDb(dataFolder)

        val mainConfigManager = ConfigurationManager(
            configFile = File("${dataFolder}/config.yml"),
            dataFolder = dataFolder,
            defaultConfig = defaultMainConfig,
            logger = logger,
            serializer = MainConfig.serializer(),
            clazz = MainConfig::class,
        )

        val charactersConfigManager = ConfigurationManager(
            configFile = File("${dataFolder}/characters.yml"),
            dataFolder = dataFolder,
            defaultConfig = defaultCharactersConfig,
            logger = logger,
            serializer = CharactersConfig.serializer(),
            clazz = CharactersConfig::class,
        )

        multiConfigurationManager = MultiConfigurationManager(
            listOf(
                mainConfigManager,
                charactersConfigManager
            )
        )

        multiConfigurationManager.loadAllConfigs().forEach { (clazz, config) ->
            ConfigRegistry.register(clazz, config)
        }
        mainConfig = ConfigRegistry.get(MainConfig::class)!!
        charactersConfig = ConfigRegistry.get(CharactersConfig::class)!!

        initializeNMSInvoker(this)

        // Initialize v2 container
        v2Container = OriginsContainer.create(this, NMSInvoker, bukkitDispatcher)
        v2Container?.let { container ->
            BuiltinModuleBootstrap.registerAll(this)
            container.initialize()

            // Push config-driven layer priorities, default origins, randomize flags.
            container.originLoader.applyMainConfig(
                layerOrders = mainConfig.originSelection.layerOrders,
                defaultOriginsByLayer = mainConfig.originSelection.defaultOrigin,
                randomizeByLayer = mainConfig.originSelection.randomize,
                orbRandomByLayer = mainConfig.orbOfOrigin.random
            )

            // Register the origin-change interceptor + post-change listeners.
            container.eventBus.registerInterceptor(OriginRestrictionInterceptor(container))
            container.eventBus.registerChangedListener(OriginUsageTracker(container))
            container.eventBus.registerChangedListener(OriginCommandDispatcher(container))

            // Register Bukkit listeners for join flow, invulnerability, death-change.
            Bukkit.getPluginManager().registerEvents(OriginJoinFlowListener(container), this)
            Bukkit.getPluginManager().registerEvents(OriginSelectionInvulnerabilityListener(container), this)
            Bukkit.getPluginManager().registerEvents(OriginDeathListener(container), this)
        }

        PacketEvents.getAPI().init()

        if (mainConfig.swapCommand.vault.enabled) {
            this.isVaultEnabled = setupEconomy()
            if (!this.isVaultEnabled) {
                logger.warning("Vault is missing, origin swaps will not cost currency")
            }
        } else this.isVaultEnabled = false

        MetricsLite(this, 24890)

        // Register event listeners
        Bukkit.getPluginManager().registerEvents(PackApplier(), this)
        Bukkit.getPluginManager().registerEvents(OrbOfOrigin(), this)
        Bukkit.getPluginManager().registerEvents(ShulkerInventoryUI, this)

        // Orb of Origin crafting recipe (gated by orbOfOrigin.enableRecipe).
        if (mainConfig.orbOfOrigin.enableRecipe) {
            OrbRecipe.register(this, mainConfig.orbOfOrigin.recipe)
        }

        // Register commands
        val originCommand = OriginCommand()
        getCommand("origin")?.setExecutor(originCommand)
        getCommand("origin")?.tabCompleter = originCommand

        // Create export/import directories
        val export = File(dataFolder, "export")
        if (!export.exists()) {
            export.mkdir()
        }
        val imports = File(dataFolder, "import")
        if (!imports.exists()) {
            imports.mkdir()
        }
    }

}
