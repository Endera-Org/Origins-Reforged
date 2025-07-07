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
import ru.turbovadim.abilities.AbilityRegister
import ru.turbovadim.abilities.custom.ToggleableAbilities
import ru.turbovadim.abilities.fantasy.*
import ru.turbovadim.abilities.main.*
import ru.turbovadim.abilities.main.WaterBreathing
import ru.turbovadim.abilities.mobs.*
import ru.turbovadim.abilities.monsters.ApplyHungerEffect
import ru.turbovadim.abilities.monsters.ApplyWitherEffect
import ru.turbovadim.abilities.monsters.BetterAim
import ru.turbovadim.abilities.monsters.BetterGoldArmour
import ru.turbovadim.abilities.monsters.BetterGoldWeapons
import ru.turbovadim.abilities.monsters.Blindness
import ru.turbovadim.abilities.monsters.BurnInDay
import ru.turbovadim.abilities.monsters.ColdSlowness
import ru.turbovadim.abilities.monsters.CreeperAlly
import ru.turbovadim.abilities.monsters.DoubleDamage
import ru.turbovadim.abilities.monsters.DoubleFireDamage
import ru.turbovadim.abilities.monsters.DoubleHealth
import ru.turbovadim.abilities.monsters.Explosive
import ru.turbovadim.abilities.monsters.FearCats
import ru.turbovadim.abilities.monsters.FreezeImmune
import ru.turbovadim.abilities.monsters.GuardianAllyMosters
import ru.turbovadim.abilities.monsters.HalfMaxSaturation
import ru.turbovadim.abilities.monsters.HeatSlowness
import ru.turbovadim.abilities.monsters.InfiniteArrows
import ru.turbovadim.abilities.monsters.LandNightVision
import ru.turbovadim.abilities.monsters.LandSlowness
import ru.turbovadim.abilities.monsters.PiglinAlly
import ru.turbovadim.abilities.monsters.ScareVillagers
import ru.turbovadim.abilities.monsters.SenseMovement
import ru.turbovadim.abilities.monsters.SkeletonBody
import ru.turbovadim.abilities.monsters.Slowness
import ru.turbovadim.abilities.monsters.SlownessArrows
import ru.turbovadim.abilities.monsters.SonicBoom
import ru.turbovadim.abilities.monsters.SuperBartering
import ru.turbovadim.abilities.monsters.SwimSpeedMonsters
import ru.turbovadim.abilities.monsters.UndeadAllyMonsters
import ru.turbovadim.abilities.monsters.UndeadMonsters
import ru.turbovadim.abilities.monsters.WaterBreathingMonsters
import ru.turbovadim.abilities.monsters.WitherImmunity
import ru.turbovadim.abilities.monsters.ZombieTouch
import ru.turbovadim.abilities.monsters.ZombifiedPiglinAllies
import ru.turbovadim.abilities.monsters.metamorphosis.DrownedTransformIntoZombie
import ru.turbovadim.abilities.monsters.metamorphosis.HuskTransformIntoZombie
import ru.turbovadim.abilities.monsters.metamorphosis.MetamorphosisTemperature
import ru.turbovadim.abilities.monsters.metamorphosis.TransformIntoHuskAndDrowned
import ru.turbovadim.abilities.monsters.metamorphosis.TransformIntoPiglin
import ru.turbovadim.abilities.monsters.metamorphosis.TransformIntoSkeleton
import ru.turbovadim.abilities.monsters.metamorphosis.TransformIntoStray
import ru.turbovadim.abilities.monsters.metamorphosis.TransformIntoZombifiedPiglin
import ru.turbovadim.abilities.types.Ability
import ru.turbovadim.abilities.types.BreakSpeedModifierAbility.BreakSpeedModifierAbilityListener
import ru.turbovadim.abilities.types.ParticleAbility
import ru.turbovadim.commands.FlightToggleCommand
import ru.turbovadim.commands.OriginCommand
import ru.turbovadim.config.*
import ru.turbovadim.cooldowns.Cooldowns
import ru.turbovadim.database.initDb
import ru.turbovadim.events.PlayerLeftClickEvent.PlayerLeftClickEventListener
import ru.turbovadim.packetsenders.*
import ru.turbovadim.util.WorldGuardHook
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

        private var cooldowns: Cooldowns? = null

        fun getCooldowns(): Cooldowns {
            return cooldowns!!
        }

        private fun initializeNMSInvoker(instance: OriginsReforged) {
            val version: String? =
                Bukkit.getBukkitVersion().split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            NMSInvoker = when (version) {
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
                "1.21.5" -> NMSInvokerV1_21_5()
                "1.21.6" -> NMSInvokerV1_21_6()
                "1.21.7" -> NMSInvokerV1_21_7()
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

        try {
            if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
                isWorldGuardHookInitialized = WorldGuardHook.tryInitialize()
            }
        } catch (_: Throwable) {
            isWorldGuardHookInitialized = false
        }
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
        ParticleAbility.initParticlesSender()
        Bukkit.getPluginManager().registerEvents(originSwapper, this)
        Bukkit.getPluginManager().registerEvents(OrbOfOrigin(), this)
        Bukkit.getPluginManager().registerEvents(PackApplier(), this)
        Bukkit.getPluginManager().registerEvents(PlayerLeftClickEventListener(), this)
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
        val abilities = mutableListOf<Ability>()
        abilities.addAll(getMainModuleAbilities())
        if (modulesConfig.fantasy) {
            abilities.addAll(getFantasyModuleAbilities())
        }
        if (modulesConfig.mobs) {
            abilities.addAll(getMobsModuleAbilities())
        }
        if (modulesConfig.monsters) {
            abilities.addAll(getMonstersModuleAbilities())
        }
        return abilities.toList()
    }

    fun getMainModuleAbilities(): List<Ability> {
        val abilities = mutableListOf(
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

        if (NMSInvoker.blockInteractionRangeAttribute != null && NMSInvoker.entityInteractionRangeAttribute != null) {
            abilities.add(ExtraReach())
            abilities.add(ExtraReach.ExtraReachBlocks.extraReachBlocks)
            abilities.add(ExtraReach.ExtraReachEntities.extraReachEntities)
        }
        abilities.addAll(ToggleableAbilities.abilities)
        return abilities
    }

    fun getFantasyModuleAbilities(): List<Ability> {
        val abilities = mutableListOf(
            AllayMaster(),
            ArrowEffectBooster(),
            BardicIntuition(),
            BowBurst(),
            BreathStorer(),
            Chime(),
            DoubleHealthFantasy(),
            DragonFireball(),
            Elegy(),
            EndCrystalHealing(),
            EndBoost(),
            FortuneIncreaser(),
            IncreasedArrowDamage(),
            IncreasedArrowSpeed(),
            HeavyBlow(),
            HeavyBlow.IncreasedCooldown,
            HeavyBlow.IncreasedDamage,
            EndBoost.EndHealth,
            EndBoost.EndStrength,
            IncreasedSpeed(),
            InfiniteHaste(),
            InfiniteNightVision(),
            OceanWish(),
            OceanWish.LandWeakness.landWeakness,
            OceanWish.LandHealth.landHealth,
            OceanWish.LandSlowness.landSlowness,
            MagicResistance(),
            MoonStrength(),
            NaturalArmor(),
            NoteBlockPower(),
            PerfectShot(),
            PermanentHorse(),
            PoorShot(),
            StrongSkin(),
            SuperJump(),
            OceansGrace(),
            OceansGrace.WaterHealthImpl,
            OceansGrace.WaterStrengthImpl,
            VampiricTransformation(),
            DaylightSensitive(),
            WaterSensitive(),
            Leeching(),
            Stronger(),
            UndeadAlly()
        )
        if (NMSInvoker.genericScaleAttribute != null) {
            abilities.add(LargeBody())
            abilities.add(SmallBody())
        }
        return abilities
    }

    fun getMobsModuleAbilities(): List<Ability> {
        return listOf(
            SmallBug(),
            SmallFox(),
            LowerTotemChance(),
            SnowTrail(),
            StrongerSnowballs(),
            BeeWings(),
            Stinger(),
            BecomesElderGuardian(),
            WarpedFungusEater(),
            WaterCombatant(),
            QueenBee(),
            Undead(),
            Sly(),
            TimidCreature(),
            PillagerAligned(),
            Illager(),
            WitchParticles(),
            MiningFatigueImmune(),
            SmallWeak(),
            SmallWeakKnockback(),
            RideableCreature(),
            GuardianAlly(),
            SurfaceSlowness(),
            SurfaceWeakness(),
            GuardianSpikes(),
            PrismarineSkin(),
            CarefulGatherer(),
            FrigidStrength(),
            BetterBerries(),
            WolfBody(),
            AlphaWolf(),
            ItemCollector(),
            BetterPotions(),
            ElderMagic(),
            ElderSpikes(),
            WaterVision(),
            SummonFangs(),
            FullMoon(),
            FullMoonHealth(),
            FullMoonAttack(),
            WolfPack(),
            WolfPackAttack(),
            ZombieHunger(),
            Temperature.INSTANCE,
            Overheat(),
            Melting(),
            MeltingSpeed(),
            WolfHowl(),
            TridentExpert(),
            FlowerPower(),
            Bouncy(),
            LavaWalk(),
            Split(),
            PotionAction()
        )
    }

    fun getMonstersModuleAbilities(): List<Ability> {
        return listOf(
            CreeperAlly(),
            Explosive(),
            FearCats(),
            DrownedTransformIntoZombie(),
            HuskTransformIntoZombie(),
            TransformIntoHuskAndDrowned(),
            TransformIntoStray(),
            TransformIntoSkeleton(),
            MetamorphosisTemperature.INSTANCE,
            Blindness(),
            SenseMovement(),
            DoubleHealth(),
            DoubleDamage(),
            SonicBoom(),
            LandNightVision(),
            DoubleFireDamage(),
            BurnInDay(),
            UndeadMonsters(),
            TridentExpert(),
            ZombieHunger(),
            WitherImmunity(),
            HalfMaxSaturation(),
            GuardianAllyMosters(),
            WaterCombatant(),
            UndeadAllyMonsters(),
            ApplyWitherEffect(),
            InfiniteArrows(),
            SlownessArrows(),
            ApplyHungerEffect(),
            SkeletonBody(),
            Slowness(),
            LandSlowness(),
            WaterBreathingMonsters(),
            SwimSpeedMonsters(),
            FreezeImmune(),
            HeatSlowness(),
            BetterAim(),
            ColdSlowness(),
            ZombieTouch(),
            ScareVillagers(),
            TransformIntoZombifiedPiglin(),
            TransformIntoPiglin(),
            BetterGoldArmour(),
            BetterGoldWeapons(),
            ZombifiedPiglinAllies(),
            SuperBartering(),
            PiglinAlly()
        )
    }
}