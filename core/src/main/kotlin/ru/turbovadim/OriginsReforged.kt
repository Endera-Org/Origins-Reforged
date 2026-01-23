package ru.turbovadim

import com.github.retrooper.packetevents.PacketEvents
import com.noxcrew.interfaces.InterfacesListeners
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.endera.enderalib.bstats.MetricsLite
import org.endera.enderalib.utils.async.BukkitDispatcher
import org.endera.enderalib.utils.configuration.ConfigurationManager
import org.endera.enderalib.utils.configuration.MultiConfigurationManager
import ru.turbovadim.commands.OriginCommand
import ru.turbovadim.config.*
import ru.turbovadim.database.initDb
import ru.turbovadim.packetsenders.*
import ru.turbovadim.v2.V2Initializer
import ru.turbovadim.v2.di.OriginsContainer
import java.io.File

class OriginsReforged : OriginsAddon() {

    companion object {

        lateinit var instance: OriginsReforged
            private set

        lateinit var multiConfigurationManager: MultiConfigurationManager
        lateinit var bukkitDispatcher: BukkitDispatcher

        lateinit var mainConfig: MainConfig
        lateinit var charactersConfig: CharactersConfig
        lateinit var modulesConfig: ModulesConfig

        lateinit var NMSInvoker: NMSInvoker
            private set

        private fun initializeNMSInvoker(instance: OriginsReforged) {
            val version =
                Bukkit.getBukkitVersion().split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            NMSInvoker = when (version) {
                "1.20" -> NMSInvokerV1_20()
                "1.20.1" -> NMSInvokerV1_20_1()
                "1.20.2" -> NMSInvokerV1_20_2()
                "1.20.3" -> NMSInvokerV1_20_3()
                "1.20.4" -> NMSInvokerV1_20_4()
                "1.20.5", "1.20.6" -> NMSInvokerV1_20_6()
                "1.21.1" -> NMSInvokerV1_21_1()
                "1.21.2", "1.21.3" -> NMSInvokerV1_21_3()
                "1.21.4" -> NMSInvokerV1_21_4()
                "1.21.5", "1.21.6" -> NMSInvokerV1_21_6()
                "1.21.7", "1.21.8" -> NMSInvokerV1_21_7()
                "1.21.9", "1.21.10" -> NMSInvokerV1_21_10()
                "1.21.11" -> NMSInvokerV1_21_11()
                else -> throw IllegalStateException("Unsupported version: " + Bukkit.getMinecraftVersion())
            }
            Bukkit.getPluginManager().registerEvents(NMSInvoker, instance)
        }

        /** v2 container - initialized in onRegister() */
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

    override fun onLoad() {
        instance = this
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().load()
    }

    override fun onDisable() {
        v2Container?.shutdown()
        PacketEvents.getAPI().terminate()
    }

    override fun onRegister() {

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

        val modulesConfigManager = ConfigurationManager(
            configFile = File("${dataFolder}/modules.yml"),
            dataFolder = dataFolder,
            defaultConfig = defaultModulesConfig,
            logger = logger,
            serializer = ModulesConfig.serializer(),
            clazz = ModulesConfig::class,
        )

        multiConfigurationManager = MultiConfigurationManager(
            listOf(
                mainConfigManager,
                charactersConfigManager,
                modulesConfigManager
            )
        )

        multiConfigurationManager.loadAllConfigs().forEach { (clazz, config) ->
            ConfigRegistry.register(clazz, config)
        }
        mainConfig = ConfigRegistry.get(MainConfig::class)!!
        charactersConfig = ConfigRegistry.get(CharactersConfig::class)!!
        modulesConfig = ConfigRegistry.get(ModulesConfig::class)!!

        initializeNMSInvoker(this)

        // Initialize v2 container
        v2Container = OriginsContainer.create(this, NMSInvoker, bukkitDispatcher)
        v2Container?.let { container ->
            V2Initializer.registerAbilities(container)
            container.initialize()
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

    override fun getNamespace(): String {
        return "origins"
    }

    override fun getAbilities(): List<ru.turbovadim.v2.ability.Ability> {
        // v2 abilities are registered directly in V2Initializer
        // Return empty list as legacy addons will use v2 registry
        return emptyList()
    }
}