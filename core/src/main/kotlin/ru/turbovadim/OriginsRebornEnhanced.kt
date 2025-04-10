package ru.turbovadim

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.endera.enderalib.bstats.MetricsLite
import org.endera.enderalib.utils.async.BukkitDispatcher
import org.endera.enderalib.utils.configuration.ConfigurationManager
import org.endera.enderalib.utils.configuration.MultiConfigurationManager
import ru.turbovadim.abilities.*
import ru.turbovadim.abilities.custom.ToggleableAbilities
import ru.turbovadim.abilities.types.Ability
import ru.turbovadim.abilities.types.BreakSpeedModifierAbility.BreakSpeedModifierAbilityListener
import ru.turbovadim.abilities.types.ParticleAbility.ParticleAbilityListener
import ru.turbovadim.commands.FlightToggleCommand
import ru.turbovadim.commands.OriginCommand
import ru.turbovadim.config.*
import ru.turbovadim.cooldowns.Cooldowns
import ru.turbovadim.database.initDb
import ru.turbovadim.events.PlayerLeftClickEvent.PlayerLeftClickEventListener
import ru.turbovadim.packetsenders.*
import ru.turbovadim.util.WorldGuardHook
import java.io.File

class OriginsRebornEnhanced : OriginsAddon() {

    companion object {

        lateinit var instance: OriginsRebornEnhanced
            private set

        lateinit var multiConfigurationManager: MultiConfigurationManager
        lateinit var bukkitDispatcher: BukkitDispatcher

        lateinit var mainConfig: MainConfig
        lateinit var charactersConfig: CharactersConfig

        lateinit var NMSInvoker: NMSInvoker
            private set

        private var cooldowns: Cooldowns? = null

        @JvmStatic
        fun getCooldowns(): Cooldowns {
            return cooldowns!!
        }

        private fun initializeNMSInvoker(instance: OriginsRebornEnhanced) {
            val version: String? =
                Bukkit.getBukkitVersion().split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            NMSInvoker = when (version) {
//                "1.18.2" -> NMSInvokerV1_18_2()
//                "1.19" -> NMSInvokerV1_19()
//                "1.19.1" -> NMSInvokerV1_19_1()
//                "1.19.2" -> NMSInvokerV1_19_2()
//                "1.19.3" -> NMSInvokerV1_19_3()
//                "1.19.4" -> NMSInvokerV1_19_4()
                "1.20" -> NMSInvokerV1_20()
                "1.20.1" -> NMSInvokerV1_20_1()
                "1.20.2" -> NMSInvokerV1_20_2()
                "1.20.3" -> NMSInvokerV1_20_3()
                "1.20.4" -> NMSInvokerV1_20_4()
                "1.20.5", "1.20.6" -> NMSInvokerV1_20_6()
                "1.21" -> NMSInvokerV1_21()
                "1.21.1" -> NMSInvokerV1_21_1()
                "1.21.2", "1.21.3" -> NMSInvokerV1_21_3()
                "1.21.4" -> NMSInvokerV1_21_4()
                else -> throw IllegalStateException("Unsupported version: " + Bukkit.getMinecraftVersion())
            }
            Bukkit.getPluginManager().registerEvents(NMSInvoker, instance)
        }

        var isWorldGuardHookInitialized: Boolean = false
            private set
    }

    var economy: Economy? = null
        private set

    private fun setupEconomy(): Boolean {
        try {
            val economyProvider = server.servicesManager.getRegistration<Economy?>(Economy::class.java)
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

        try {
            if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
                isWorldGuardHookInitialized = WorldGuardHook.tryInitialize()
            }
        } catch (_: Throwable) {
            isWorldGuardHookInitialized = false
        }
    }

    override fun onOnEnable() {

    }

    override fun onDisable() {
        PacketEvents.getAPI().terminate()
    }

    override fun onRegister() {
        bukkitDispatcher = BukkitDispatcher(this)
        initDb(dataFolder)
        if (isWorldGuardHookInitialized) WorldGuardHook.completeInitialize()

        ToggleableAbilities.initialize(this)

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

//        saveDefaultConfig()
        initializeNMSInvoker(this)
        AbilityRegister.setupAMAF()


        PacketEvents.getAPI().eventManager.registerListener(Unwieldy(), PacketListenerPriority.NORMAL)
        PacketEvents.getAPI().eventManager.registerListener(SlowFalling(), PacketListenerPriority.NORMAL)
        PacketEvents.getAPI().eventManager.registerListener(LikeWater(), PacketListenerPriority.NORMAL)

        PacketEvents.getAPI().init()

        if (mainConfig.swapCommand.vault.enabled) {
            this.isVaultEnabled = setupEconomy()
            if (!this.isVaultEnabled) {
                logger.warning("Vault is missing, origin swaps will not cost currency")
            }
        } else this.isVaultEnabled = false
        cooldowns = Cooldowns()
        if (!mainConfig.cooldowns.disableAllCooldowns && mainConfig.cooldowns.showCooldownIcons) {
            Bukkit.getPluginManager().registerEvents(cooldowns!!, this)
        }

        MetricsLite(this, 24890)

        val originSwapper = OriginSwapper()
        Bukkit.getPluginManager().registerEvents(originSwapper, this)
        Bukkit.getPluginManager().registerEvents(OrbOfOrigin(), this)
        Bukkit.getPluginManager().registerEvents(PackApplier(), this)
        Bukkit.getPluginManager().registerEvents(PlayerLeftClickEventListener(), this)
        Bukkit.getPluginManager().registerEvents(ParticleAbilityListener(), this)
        Bukkit.getPluginManager().registerEvents(BreakSpeedModifierAbilityListener(), this)
        originSwapper.startScheduledTask()

        val flightCommand = getCommand("fly")
        flightCommand?.setExecutor(FlightToggleCommand())

        val export = File(dataFolder, "export")
        if (!export.exists()) {
            export.mkdir()
        }
        val imports = File(dataFolder, "import")
        if (!imports.exists()) {
            imports.mkdir()
        }

        val command = getCommand("origin")
        command?.setExecutor(OriginCommand())
    }

    override fun getNamespace(): String {
        return "origins"
    }

    override fun getAbilities(): List<Ability> {
        val abilities: MutableList<Ability> = ArrayList(
            mutableListOf(
                PumpkinHate(),
                FallImmunity(),
                WeakArms(),
                Fragile(),
                SlowFalling(),
                FreshAir(),
                Vegetarian(),
                LayEggs(),
                Unwieldy(),
                MasterOfWebs(),
                Tailwind(),
                Arthropod(),
                Climbing(),
                Carnivore(),
                WaterBreathing(),
                WaterVision(),
                CatVision(),
                NineLives(),
                BurnInDaylight(),
                WaterVulnerability(),
                Phantomize(),
                Invisibility(),
                ThrowEnderPearl(),
                PhantomizeOverlay(),
                FireImmunity(),
                AirFromPotions(),
                SwimSpeed(),
                LikeWater(),
                LightArmor(),
                MoreKineticDamage(),
                DamageFromPotions(),
                DamageFromSnowballs(),
                Hotblooded(),
                BurningWrath(),
                SprintJump(),
                AerialCombatant(),
                Elytra(),
                LaunchIntoAir(),
                HungerOverTime(),
                MoreExhaustion(),
                Aquatic(),
                NetherSpawn(),
                Claustrophobia(),
                VelvetPaws(),
                AquaAffinity(),
                FlameParticles(),
                EnderParticles(),
                Phasing(),
                ScareCreepers(),
                StrongArms(),
                StrongArms.StrongArmsBreakSpeed.strongArmsBreakSpeed,
                StrongArms.StrongArmsDrops.strongArmsDrops,
                ShulkerInventory(),
                NaturalArmor()
            )
        )
        if (NMSInvoker.blockInteractionRangeAttribute != null && NMSInvoker.entityInteractionRangeAttribute != null) {
            abilities.add(ExtraReach())
            abilities.add(ExtraReach.ExtraReachBlocks.extraReachBlocks)
            abilities.add(ExtraReach.ExtraReachEntities.extraReachEntities)
        }
        abilities.addAll(ToggleableAbilities.abilities)
        return abilities.toList()
    }
}