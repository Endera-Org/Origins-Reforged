package ru.turbovadim.config

import kotlinx.serialization.Serializable
import org.endera.enderalib.utils.configuration.Comment
import org.endera.enderalib.utils.configuration.Spacer

@Serializable
data class MainConfig(
    @Comment("Worlds used for some abilities")
    val worlds: Worlds,

    @Spacer(1)
    @Comment("""
        Runs commands when the player switches to an origin
        Origins should be formatted as they are in the file names, but without the extension, e.g. "human"
        %player% is replaced with the player's username and %uuid% is replaced with their UUID
        Use "default" for commands that should be run regardless of origin
    """)
    val commandsOnOrigin: Map<String, List<String>>,

    @Spacer(1)
    @Comment("A list of WorldGuard regions in which to prevent the use of certain abilities, use 'all' for all abilities")
    val preventAbilitiesIn: Map<String, List<String>>,

    @Spacer(1)
    @Comment("""
        Disables every cooldown
        To modify specific cooldowns, edit the cooldown-config.yml file
    """)
    val cooldowns: Cooldowns,

    @Spacer(1)
    @Comment("Miscellaneous settings")
    val miscSettings: MiscSettings,

    @Spacer(1)
    @Comment("The /origin swap command, allowing players to switch origin at will")
    val swapCommand: SwapCommand,

    @Spacer(1)
    @Comment("Settings for origin selection")
    val originSelection: OriginSelection,

    @Spacer(1)
    @Comment("Settings for the Orb of Origin")
    val orbOfOrigin: OrbOfOrigin,

    @Spacer(1)
    @Comment("""
        Whether to enable the resource pack
        If this is set to false you should send the pack to players either in server.properties or in another plugin
        You can find the packs for each version on the GitHub at https://github.com/cometcake575/Origins-Reborn/tree/main/packs/
    """)
    val resourcePack: ResourcePack,

    @Spacer(1)
    @Comment("Miscellaneous display options")
    val display: Display,

    @Spacer(1)
    @Comment("""
        Rule for reusing origins
        "NONE" allows origins to be reused
        "PERPLAYER" means individual players can only use an origin once
        "ALL" means no players can use an origin again after one has selected it
    """)
    val restrictions: Restrictions,

    @Spacer(1)
    @Comment("Configure plugin messages")
    val messages: Messages,

    @Spacer(1)
    @Comment("Settings for using GeyserMC")
    val geyser: Geyser,

    @Spacer(1)
    @Comment("Extra settings for abilities")
    val extraSettings: ExtraSettings,

    @Spacer(1)
    @Comment("Config version - do not touch this!")
    val configVersion: String
)

// --- Nested Data Classes ---

@Serializable
data class Worlds(
    @Comment("Overworld dimension")
    val world: String,

    @Comment("Nether dimension")
    val worldNether: String,

    @Comment("End dimension")
    val worldTheEnd: String,

    @Comment("Worlds to disable origins in")
    val disabledWorlds: List<String>
)

@Serializable
data class Cooldowns(
    @Comment("""
        Disables every cooldown
        To modify specific cooldowns, edit the cooldown-config.yml file
    """)
    val disableAllCooldowns: Boolean,

    @Comment("""
        Use the actionbar to show cooldown icons
        You may want to disable this if using another plugin that requires the actionbar
    """)
    val showCooldownIcons: Boolean
)

@Serializable
data class MiscSettings(
    @Comment("Disable all flight-related features. This does not hide the abilities themselves, they must be removed from the .yml files in the ~/plugins/Origins-Reborn/origins/ folder")
    val disableFlightStuff: Boolean
)

@Serializable
data class SwapCommand(
    @Comment("Enable the swap command")
    val enabled: Boolean,

    @Comment("Permission required for origin swap command")
    val permission: String,

    @Comment("Reset player data like inventory and spawn point when switching origins using the /origin swap command")
    val resetPlayer: Boolean,

    @Comment("Charge players using Vault to switch their origins")
    val vault: Vault
)

@Serializable
data class Vault(
    @Comment("Enable charging players with Vault")
    val enabled: Boolean,

    @Comment("Permission to bypass the cost of the swap command")
    val bypassPermission: String,

    @Comment("Default cost of switching origin, if it hasn't been overriden in the origin file")
    val defaultCost: Int,

    @Comment("Allows the player to switch back to origins for free if they already had the origin before")
    val permanentPurchases: Boolean,

    @Comment("Currency symbol for the economy")
    val currencySymbol: String
)

@Serializable
data class OriginSelection(
    @Comment("The amount of time (in ticks, a tick is a 20th of a second) to wait between a player joining and when the GUI should open")
    val delayBeforeRequired: Int,

    @Comment("Priorities for different origin 'layers' to be selected in, higher priority layers are selected first.")
    val layerOrders: Map<String, Int>,

    @Comment("""
        Default origin, automatically gives players this origin rather than opening the GUI when the player has no origin
        Should be the name of the origin file without the ending, e.g. for 'origin_name.json' the value should be 'origin_name'
        Disabled if set to an invalid name such as "NONE"
    """)
    val defaultOrigin: Map<String, String>,

    @Comment("""
        OFF - you can take damage with the origin selection GUI open
        ON - you cannot take damage with the origin selection GUI open
        INITIAL - you cannot take damage if you do not have an origin (and therefore cannot close the screen)
    """)
    val invulnerableMode: String,

    @Comment("Automatically teleport players to the world spawn when first selecting an origin")
    val autoSpawnTeleport: Boolean,

    @Comment("Randomize origins instead of letting players pick")
    val randomize: Map<String, Boolean>,

    @Comment("""
        Allows players to choose a new origin when they die
        If randomise is enabled this will reroll their origin to something random
    """)
    val deathOriginChange: Boolean,

    @Comment("Amount to scroll per scroll button click")
    val scrollAmount: Int,

    @Comment("Allows the player to pick a 'Random' option")
    val randomOption: RandomOption,

    @Comment("""
        Prefixes and suffixes for the selection screen title
        This is an advanced setting - only use it if you know how
    """)
    val screenTitle: ScreenTitle
)

@Serializable
data class RandomOption(
    @Comment("Enable the random option choice - does nothing if randomise is enabled")
    val enabled: Boolean,

    @Comment("Origins to exclude from random options")
    val exclude: List<String>
)

@Serializable
data class ScreenTitle(
    @Comment("Prefix of GUI title")
    val prefix: String,

    @Comment("Background, between Origins-Reborn background and things like text")
    val background: String,

    @Comment("Suffix of GUI title")
    val suffix: String
)

@Serializable
data class OrbOfOrigin(
    @Comment("Reset player data like inventory and spawn point when switching origins using the Orb of Origin")
    val resetPlayer: Boolean,

    @Comment("Consume the Orb of Origin upon use")
    val consume: Boolean,

    @Comment("Enable recipe for crafting the Orb of Origin")
    val enableRecipe: Boolean,

    @Comment("Randomise origin instead of opening the selector upon using the orb")
    val random: Map<String, Boolean>,

    @Comment("Crafting recipe for the Orb of Origin")
    val recipe: List<List<String>>
)

@Serializable
data class ResourcePack(
    @Comment("""
        Whether to enable the resource pack
        If this is set to false you should send the pack to players either in server.properties or in another plugin
        You can find the packs for each version on the GitHub at https://github.com/cometcake575/Origins-Reborn/tree/main/packs/
    """)
    val enabled: Boolean
)

@Serializable
data class Display(
    @Comment("Enable prefixes in tab and on display names with the names of origins")
    val enablePrefixes: Boolean
)

@Serializable
data class Restrictions(
    @Comment("""
        Rule for reusing origins
        "NONE" allows origins to be reused
        "PERPLAYER" means individual players can only use an origin once
        "ALL" means no players can use an origin again after one has selected it
    """)
    val reusingOrigins: String,

    @Comment("Prevent players from having the same origins as other players\nThis is locked on if reusing-origins is set to ALL")
    val preventSameOrigins: Boolean
)

@Serializable
data class Messages(
    @Comment("Player used /origin swap and does not have permission")
    val noSwapCommandPermissions: String
)

@Serializable
data class Geyser(
    @Comment("The delay in ticks to wait before showing a new Bedrock player the selection GUI")
    val joinFormDelay: Int
)

@Serializable
data class ExtraSettings(
    @Comment("Required sleep height for origins with the Fresh Air ability")
    val freshAirRequiredSleepHeight: Int
)

// --- Default Configuration Instance ---
val defaultMainConfig = MainConfig(
    worlds = Worlds(
        world = "world",
        worldNether = "world_nether",
        worldTheEnd = "world_the_end",
        disabledWorlds = listOf("example_world")
    ),
    commandsOnOrigin = mapOf(
        "example" to listOf("example %player%", "example %uuid%")
    ),
    preventAbilitiesIn = mapOf(
        "no-water-breathing" to listOf("water_breathing"),
        "no-abilities" to listOf("all")
    ),
    cooldowns = Cooldowns(
        disableAllCooldowns = false,
        showCooldownIcons = true
    ),
    miscSettings = MiscSettings(
        disableFlightStuff = false
    ),
    swapCommand = SwapCommand(
        enabled = true,
        permission = "originsreborn.admin",
        resetPlayer = false,
        vault = Vault(
            enabled = false,
            bypassPermission = "originsreborn.costbypass",
            defaultCost = 1000,
            permanentPurchases = false,
            currencySymbol = "$"
        )
    ),
    originSelection = OriginSelection(
        delayBeforeRequired = 0,
        layerOrders = mapOf("origin" to 1),
        defaultOrigin = mapOf("origin" to "NONE"),
        invulnerableMode = "OFF",
        autoSpawnTeleport = true,
        randomize = mapOf("origin" to false),
        deathOriginChange = false,
        scrollAmount = 1,
        randomOption = RandomOption(
            enabled = true,
            exclude = listOf("human")
        ),
        screenTitle = ScreenTitle(
            prefix = "",
            background = "",
            suffix = ""
        )
    ),
    orbOfOrigin = OrbOfOrigin(
        resetPlayer = false,
        consume = true,
        enableRecipe = false,
        random = mapOf("origin" to false),
        recipe = listOf(
            listOf("minecraft:air", "minecraft:diamond", "minecraft:air"),
            listOf("minecraft:diamond", "minecraft:nether_star", "minecraft:diamond"),
            listOf("minecraft:air", "minecraft:diamond", "minecraft:air")
        )
    ),
    resourcePack = ResourcePack(
        enabled = true
    ),
    display = Display(
        enablePrefixes = false
    ),
    restrictions = Restrictions(
        reusingOrigins = "NONE",
        preventSameOrigins = false
    ),
    messages = Messages(
        noSwapCommandPermissions = "§cYou don't have permission to do this!"
    ),
    geyser = Geyser(
        joinFormDelay = 20
    ),
    extraSettings = ExtraSettings(
        freshAirRequiredSleepHeight = 86
    ),
    configVersion = "2.4.2"
)