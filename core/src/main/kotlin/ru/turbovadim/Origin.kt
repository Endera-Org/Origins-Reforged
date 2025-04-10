package ru.turbovadim

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import ru.turbovadim.abilities.AbilityRegister
import ru.turbovadim.abilities.types.Ability
import ru.turbovadim.abilities.types.MultiAbility
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.database.DatabaseManager
import java.util.*

class Origin(
    private val name: String,
    val icon: ItemStack,
    val position: Int,
    @get:Range(from = 0, to = 3) impactParam: Int,
    val displayName: String,
    private val abilities: List<Key>,
    private val description: String,
    val addon: OriginsAddon,
    private val unchoosable: Boolean,
    val priority: Int,
    val permission: String?,
    val cost: Int?, // may be null
    private val max: Int,
    val layer: String
) {
    val team: Team? = if (OriginsRebornEnhanced.mainConfig.display.enablePrefixes) {
        val scoreboard: Scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        scoreboard.getTeam(name)?.unregister()
        val newTeam = scoreboard.registerNewTeam(name)
        newTeam.displayName(Component.text("[")
            .color(NamedTextColor.DARK_GRAY)
            .append(Component.text(name).color(NamedTextColor.WHITE))
            .append(Component.text("] ").color(NamedTextColor.DARK_GRAY))
        )
        newTeam
    } else {
        null
    }

    val impact: Char = when (impactParam) {
        0 -> '\uE002'
        1 -> '\uE003'
        2 -> '\uE004'
        else -> '\uE005'
    }

    suspend fun isUnchoosable(player: Player): Boolean {
        if (unchoosable) return true
        val mode = OriginsRebornEnhanced.mainConfig.restrictions.reusingOrigins
        val same = OriginsRebornEnhanced.mainConfig.restrictions.preventSameOrigins

        if (max != -1) {
            var num = 0
            DatabaseManager.getAllUsedOrigins().forEach {
                if (it.equals(getName().lowercase(Locale.getDefault()), ignoreCase = true)) {
                    num++
                }
            }
            if (num >= max) return true
        }
        if (same) {
            return DatabaseManager.getAllUsedOrigins().contains(getName().lowercase(Locale.getDefault()))
        }
        return when (mode) {
            "PERPLAYER" -> DatabaseManager
                .getUsedOrigins(player.uniqueId.toString())
                .contains(getName().lowercase(Locale.getDefault()))
            "ALL" -> {
                DatabaseManager.getAllUsedOrigins().contains(getName().lowercase(Locale.getDefault()))
            }
            else -> false
        }
    }

    fun hasPermission(): Boolean = permission != null


    fun getNameForDisplay(): String = displayName


    fun getVisibleAbilities(): List<VisibleAbility> {
        val result = mutableListOf<VisibleAbility>()
        for (key in abilities) {
            val ability = AbilityRegister.abilityMap[key]
            if (ability is VisibleAbility) result.add(ability)
        }
        return result
    }

    fun getAbilities(): List<Ability> {
        val originAbilities = mutableListOf<Ability>()
        for (key in abilities) {
            val a = AbilityRegister.abilityMap[key]
            if (a == null) continue
            originAbilities.add(a)
            if (a is MultiAbility) originAbilities.addAll(a.abilities)
        }
        return originAbilities
    }

    fun hasAbility(key: Key): Boolean {
        for (ability in AbilityRegister.multiAbilityMap.getOrDefault(key, listOf())) {
            if (abilities.contains(ability.getKey())) return true
        }
        return abilities.contains(key)
    }

    fun getName(): String {
        return AddonLoader.getTextFor(
            "origin.${addon.getNamespace()}.${name.replace(" ", "_").lowercase(Locale.getDefault())}.name",
            name
        )
    }

    fun getActualName(): String = name

    fun getDescription(): String {
        return AddonLoader.getTextFor(
            "origin.${addon.getNamespace()}.${name.replace(" ", "_").lowercase(Locale.getDefault())}.description",
            description
        )
    }

    fun getResourceURL(): String {
        val keyValue = icon.type.key.value()
        val folder = if (icon.type.isBlock) "block" else "item"
        return "https://assets.mcasset.cloud/1.20.4/assets/minecraft/textures/$folder/$keyValue.png"
    }
}
