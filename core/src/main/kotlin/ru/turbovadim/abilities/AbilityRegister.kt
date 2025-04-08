package ru.turbovadim.abilities

import net.kyori.adventure.key.Key
import net.kyori.adventure.util.TriState
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginsRebornEnhanced
import ru.turbovadim.commands.FlightToggleCommand
import ru.turbovadim.cooldowns.CooldownAbility
import ru.turbovadim.packetsenders.NMSInvoker
import java.io.File
import java.io.IOException

object AbilityRegister {
    val abilityMap: MutableMap<Key, Ability> = mutableMapOf()
    val dependencyAbilityMap: MutableMap<Key, DependencyAbility> = mutableMapOf()
    val multiAbilityMap: MutableMap<Key, MutableList<MultiAbility>> = mutableMapOf()

    val origins: OriginsRebornEnhanced = OriginsRebornEnhanced.instance
    val nmsInvoker: NMSInvoker = OriginsRebornEnhanced.NMSInvoker

    lateinit var attributeModifierAbilityFileConfig: FileConfiguration
    private lateinit var attributeModifierAbilityFile: File

    fun registerAbility(ability: Ability, instance: JavaPlugin) {
        if (ability is DependencyAbility) {
            dependencyAbilityMap[ability.getKey()] = ability
        }

        if (ability is MultiAbility) {
            ability.abilities.forEach { a ->
                multiAbilityMap.getOrPut(a.getKey()) { mutableListOf() }.add(ability)
            }
        }

        if (ability is CooldownAbility) {
            OriginsRebornEnhanced.getCooldowns().registerCooldown(
                instance,
                ability.cooldownKey,
                requireNotNull(ability.cooldownInfo)
            )
        }

        if (ability is Listener) {
            Bukkit.getPluginManager().registerEvents(ability, instance)
        }

        if (ability is AttributeModifierAbility) {
            val formattedValueKey = "${ability.getKey()}.value"
            val formattedOperationKey = "${ability.getKey()}.operation"
            var changed = false

            if (!attributeModifierAbilityFileConfig.contains(ability.getKey().toString())) {
                attributeModifierAbilityFileConfig.set(formattedValueKey, "x")
                attributeModifierAbilityFileConfig.set(formattedOperationKey, "default")
                changed = true
            }

            if ("default" == attributeModifierAbilityFileConfig.get(formattedValueKey, "default")) {
                attributeModifierAbilityFileConfig.set(formattedValueKey, "x")
                changed = true
            }

            if (changed) {
                try {
                    attributeModifierAbilityFileConfig.save(attributeModifierAbilityFile)
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
        }

        abilityMap[ability.getKey()] = ability
    }

//    @Deprecated("Testing abilities is now contained in the Ability interface")
//    fun runForAbility(entity: Entity?, key: Key, runnable: Runnable) {
//        runForAbility(entity, key, runnable, Runnable { })
//    }
//
//    @Deprecated("Testing abilities is now contained in the Ability interface")
//    fun hasAbility(player: Player, key: Key): Boolean {
//        return hasAbility(player, key, false)
//    }
//
//    @Deprecated("Testing abilities is now contained in the Ability interface")
//    fun hasAbility(player: Player, key: Key, ignoreOverrides: Boolean): Boolean {
//        if (!abilityMap.containsKey(key)) return false
//        return abilityMap[key]!!.hasAbility(player)
//    }
//
//    @Deprecated("Testing abilities is now contained in the Ability interface")
//    fun runForAbility(entity: Entity?, key: Key, runnable: Runnable, other: Runnable) {
//        if (entity == null) return
//        val worldId = entity.world.name
//        if (OriginsReborn.mainConfig.worlds.disabledWorlds.contains(worldId)) return
//        if (entity is Player) {
//            if (hasAbility(entity, key)) {
//                runnable.run()
//                return
//            }
//        }
//        other.run()
//    }
//
//    @Deprecated("Testing abilities is now contained in the Ability interface")
//    fun runWithoutAbility(entity: Entity?, key: Key, runnable: Runnable) {
//        runForAbility(entity, key, Runnable { }, runnable)
//    }

    fun canFly(player: Player, disabledWorld: Boolean): Boolean {
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR || FlightToggleCommand.canFly(player))
            return true
        if (disabledWorld) return false
        for (ability in abilityMap.values) {
            if (ability is FlightAllowingAbility) {
                if (ability.hasAbility(player) && ability.canFly(player)) return true
            }
        }
        return false
    }

    fun isInvisible(player: Player): Boolean {
        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) return true
        for (ability in abilityMap.values) {
            if (ability is VisibilityChangingAbility) {
                if (ability.hasAbility(player) && ability.isInvisible(player)) return true
            }
        }
        return false
    }

    fun updateFlight(player: Player, inDisabledWorld: Boolean) {
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) {
            return
        }
        if (FlightToggleCommand.canFly(player)) {
            player.flySpeed = 0.1f
            return
        }
        if (inDisabledWorld) return

        var flyingFallDamage: TriState = TriState.FALSE
        var speed = -1f // Using -1 as an indicator of missing ability

        for (ability in abilityMap.values) {
            if (ability !is FlightAllowingAbility) continue
            if (!ability.hasAbility(player) || !ability.canFly(player)) continue

            val abilitySpeed = ability.getFlightSpeed(player)
            speed = if (speed < 0f) abilitySpeed else minOf(speed, abilitySpeed)

            if (ability.getFlyingFallDamage(player) == TriState.TRUE) {
                flyingFallDamage = TriState.TRUE
            }
        }

        nmsInvoker.setFlyingFallDamage(player, flyingFallDamage)
        player.flySpeed = if (speed < 0f) 0f else speed
    }

    fun updateEntity(player: Player, target: Entity) {
        var data: Byte = 0

        if (target.fireTicks > 0) {
            data = (data.toInt() or 0x01).toByte()
        }
        if (target.isGlowing) {
            data = (data.toInt() or 0x40).toByte()
        }
        if (target is LivingEntity && target.isInvisible) {
            data = (data.toInt() or 0x20).toByte()
        }
        if (target is Player) {
            if (target.isSneaking) {
                data = (data.toInt() or 0x02).toByte()
            }
            if (target.isSprinting) {
                data = (data.toInt() or 0x08).toByte()
            }
            if (target.isSwimming) {
                data = (data.toInt() or 0x10).toByte()
            }
            if (target.isGliding) {
                data = (data.toInt() or 0x80).toByte()
            }

            val inventory = target.inventory
            for (slot in EquipmentSlot.entries) {
                try {
                    val item: ItemStack? = inventory.getItem(slot)
                    if (item != null) {
                        player.sendEquipmentChange(target, slot, item)
                    }
                } catch (ignored: IllegalArgumentException) {
                    // If the slot is not supported, skip it
                }
            }
        }

        nmsInvoker.sendEntityData(player, target, data)
    }

    fun setupAMAF() {
        attributeModifierAbilityFile = File(origins.dataFolder, "attribute-modifier-ability-config.yml")
        if (!attributeModifierAbilityFile.exists()) {
            attributeModifierAbilityFile.parentFile.mkdirs()
            origins.saveResource("attribute-modifier-ability-config.yml", false)
        }

        attributeModifierAbilityFileConfig = YamlConfiguration()
        try {
            attributeModifierAbilityFileConfig.load(attributeModifierAbilityFile)
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: InvalidConfigurationException) {
            throw RuntimeException(e)
        }
    }
}
