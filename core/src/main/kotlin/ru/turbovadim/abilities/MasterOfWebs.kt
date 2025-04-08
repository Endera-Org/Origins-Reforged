package ru.turbovadim.abilities

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapelessRecipe
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsRebornEnhanced.Companion.NMSInvoker
import ru.turbovadim.OriginsRebornEnhanced.Companion.instance
import ru.turbovadim.abilities.types.Ability.AbilityRunner
import ru.turbovadim.abilities.types.FlightAllowingAbility
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.cooldowns.CooldownAbility
import ru.turbovadim.cooldowns.Cooldowns.CooldownInfo

class MasterOfWebs : CooldownAbility, FlightAllowingAbility, Listener, VisibleAbility {
    private val glowingEntities: MutableMap<Player?, MutableList<Entity?>?> = HashMap<Player?, MutableList<Entity?>?>()

    private val temporaryCobwebs: MutableList<Location?> = ArrayList<Location?>()

    @EventHandler
    fun onBlockDropItem(event: BlockDropItemEvent) {
        if (temporaryCobwebs.contains(event.getBlock().location)) {
            event.isCancelled = true
            temporaryCobwebs.remove(event.getBlock().location)
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        runForAbility(event.damager, AbilityRunner { player: Player? ->
            if (hasCooldown(player!!)) return@AbilityRunner
            if (!event.getEntity().location.block.isSolid) {
                setCooldown(player)
                val location = event.getEntity().location.block.location
                temporaryCobwebs.add(location)
                location.block.type = Material.COBWEB
                Bukkit.getScheduler().scheduleSyncDelayedTask(instance, Runnable {
                    if (location.block.type == Material.COBWEB && temporaryCobwebs.contains(location)) {
                        temporaryCobwebs.remove(location)
                        location.block.type = Material.AIR
                    }
                }, 60)
            }
        })
    }


    private fun setCanFly(player: Player, setFly: Boolean) {
        if (setFly) player.allowFlight = true
        canFly.put(player, setFly)
    }

    private val canFly: MutableMap<Player?, Boolean?> = HashMap<Player?, Boolean?>()


    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent?) {
        val onlinePlayers = Bukkit.getOnlinePlayers()

        for (player in onlinePlayers) {
            runForAbility(player, AbilityRunner { webMaster: Player ->
                val inCobweb = isInCobweb(webMaster)
                setCanFly(webMaster, inCobweb)
                if (inCobweb) webMaster.isFlying = true

                val entities = (webMaster.getNearbyEntities(16.0, 16.0, 16.0)
                    .filterIsInstance<LivingEntity>()
                    .take(16) + onlinePlayers)
                    .distinct()
                    .filter { it.world == webMaster.world && it.location.distance(webMaster.location) <= 16 }

                for (entity in entities) {
                    runForAbility(entity, null) { webStuck: Player ->
                        if (webStuck !== webMaster) {
                            val masterEntities = glowingEntities.getOrPut(webMaster) { ArrayList() }!!
                            if (isInCobweb(webStuck)) {
                                if (!masterEntities.contains(webStuck)) {
                                    masterEntities.add(webStuck)
                                }
                                NMSInvoker.sendEntityData(webMaster, webStuck, getData(webStuck))
                            } else {
                                masterEntities.remove(webStuck)
                                AbilityRegister.updateEntity(webMaster, webStuck)
                            }
                        }
                    }
                }
            })
        }
    }


    init {
        val recipeKey = NamespacedKey(instance, "web-recipe")
        val webRecipe = ShapelessRecipe(recipeKey, ItemStack(Material.COBWEB))
        if (Bukkit.getRecipe(recipeKey) == null) {
            webRecipe.addIngredient(Material.STRING)
            webRecipe.addIngredient(Material.STRING)
            Bukkit.addRecipe(webRecipe)
        }
    }

    @EventHandler
    fun onPrepareItemCraft(event: PrepareItemCraftEvent) {
        if (event.recipe != null) {
            if (event.recipe!!.result.type == Material.COBWEB) {
                for (entity in event.inventory.viewers) {
                    runForAbility(
                        entity,
                        null,
                        AbilityRunner { player: Player? -> event.inventory.result = null })
                }
            }
        }
    }

    override fun getKey(): Key {
        return Key.key("origins:master_of_webs")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "You navigate cobweb perfectly, and are able to climb in them. When you hit an enemy in melee, they get stuck in cobweb for a while. Non-arthropods stuck in cobweb will be sensed by you. You are able to craft cobweb from string.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Master of Webs",
        LineComponent.LineType.TITLE
    )

    fun isInCobweb(entity: Entity): Boolean {
        for (start in object : ArrayList<Block?>() {
            init {
                add(entity.location.block)
                add(entity.location.block.getRelative(BlockFace.UP))
            }
        }) {
            if (start!!.type == Material.COBWEB) return true
            for (face in BlockFace.entries) {
                val block = start.getRelative(face)
                if (block.type != Material.COBWEB) continue
                if (entity.boundingBox.overlaps(block.boundingBox)) {
                    return true
                }
            }
        }
        return false
    }

    override fun canFly(player: Player): Boolean {
        return canFly.getOrDefault(player, false)!!
    }

    override fun getFlightSpeed(player: Player): Float {
        return 0.04f
    }

    override val cooldownInfo: CooldownInfo
        get() = CooldownInfo(120, "web")

    companion object {
        private fun getData(webStuck: Entity): Byte {
            var data: Byte = 0x40
            if (webStuck.fireTicks > 0) {
                data = (data + 0x01).toByte()
            }
            if (webStuck is LivingEntity) {
                if (webStuck.isInvisible) data = (data + 0x20).toByte()
            }
            if (webStuck is Player) {
                if (webStuck.isSneaking) {
                    data = (data + 0x02).toByte()
                }
                if (webStuck.isSprinting) {
                    data = (data + 0x08).toByte()
                }
                if (webStuck.isSwimming) {
                    data = (data + 0x10).toByte()
                }
                if (webStuck.isGliding) {
                    data = (data + 0x80.toByte()).toByte()
                }
            }
            return data
        }
    }
}
