package ru.turbovadim.abilities.types

import com.destroystokyo.paper.MaterialTags
import com.destroystokyo.paper.event.server.ServerTickEndEvent
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.FluidCollisionMode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.potion.PotionEffect
import ru.turbovadim.Origin
import ru.turbovadim.OriginSwapper.Companion.getOrigins
import ru.turbovadim.OriginsRebornEnhanced.Companion.NMSInvoker
import ru.turbovadim.OriginsRebornEnhanced.Companion.instance
import ru.turbovadim.SavedPotionEffect
import ru.turbovadim.ShortcutUtils.infiniteDuration
import ru.turbovadim.abilities.StrongArms.StrongArmsBreakSpeed.StrongArmsFastBlockBreakEvent
import ru.turbovadim.packetsenders.OriginsRebornBlockDamageAbortEvent
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.roundToInt

interface BreakSpeedModifierAbility : Ability {
    fun provideContextFor(player: Player): BlockMiningContext
    fun shouldActivate(player: Player): Boolean

    data class BlockMiningContext(
        val heldItem: ItemStack,
        val slowDigging: PotionEffect?,
        val fastDigging: PotionEffect?,
        val conduitPower: PotionEffect?,
        val underwater: Boolean,
        val aquaAffinity: Boolean,
        val onGround: Boolean
    ) {
        fun hasDigSpeed(): Boolean {
            return fastDigging != null || conduitPower != null
        }

        fun hasDigSlowdown(): Boolean {
            return slowDigging != null
        }

        val digSlowdown: Int
            get() {
                if (slowDigging == null) return 0
                return slowDigging.amplifier
            }

        val digSpeedAmplification: Int
            get() {
                var i = 0
                var j = 0
                if (fastDigging != null) {
                    i = fastDigging.amplifier
                }
                if (conduitPower != null) {
                    j = conduitPower.amplifier
                }
                return max(i.toDouble(), j.toDouble()).toInt()
            }
    }

    class BreakSpeedModifierAbilityListener : Listener {
        var random: Random = Random()

        @EventHandler
        fun onBlockDamage(event: BlockDamageEvent) {
            if (event.getBlock().type.getHardness() < 0) return
            Bukkit.getScheduler().scheduleSyncDelayedTask(instance, Runnable {
                val origins: MutableList<Origin> = runBlocking {
                    getOrigins(event.player)
                }
                val abilities: MutableList<Ability?> = ArrayList<Ability?>()
                for (origin in origins) abilities.addAll(origin.getAbilities())
                var speedModifierAbility: BreakSpeedModifierAbility? = null
                for (ability in abilities) {
                    if (ability is BreakSpeedModifierAbility) {
                        if (ability.shouldActivate(event.player)) {
                            speedModifierAbility = ability
                            break
                        }
                    }
                }
                if (speedModifierAbility == null) return@Runnable
                val time = AtomicInteger()
                val marker =
                    event.player.world.spawnEntity(event.player.location, EntityType.MARKER)
                val finalSpeedModifierAbility: BreakSpeedModifierAbility? = speedModifierAbility
                val task = Bukkit.getScheduler().scheduleSyncRepeatingTask(instance, Runnable {
                    try {
                        val context = finalSpeedModifierAbility!!.provideContextFor(event.player)
                        val damage = getBlockDamage(event.getBlock(), context, time.getAndIncrement())
                        if (damage >= 1) {
                            val taskNum: Int = blockbreakingTasks[event.player]!!
                            cancelTask(taskNum)
                            val blockBreakEvent: BlockBreakEvent =
                                StrongArmsFastBlockBreakEvent(event.getBlock(), event.player)
                            blockBreakEvent.callEvent()
                            val handItem = event.player.inventory.itemInMainHand
                            if (isTool(handItem.type)) {
                                val unbreakingLevel =
                                    handItem.getEnchantmentLevel(NMSInvoker.unbreakingEnchantment) + 1
                                var itemDamage = 0
                                if (random.nextDouble() <= 1.0 / unbreakingLevel) {
                                    itemDamage += 1
                                }
                                if (event.getBlock().getDrops(context.heldItem).isEmpty()) {
                                    if (random.nextDouble() <= 1.0 / unbreakingLevel) {
                                        itemDamage += 1
                                    }
                                }
                                val damageable = handItem.itemMeta as? Damageable
                                if (damageable is Damageable) {
                                    damageable.damage = damageable.damage + itemDamage
                                    if (handItem.type.maxDurability <= damageable.damage) {
                                        NMSInvoker.broadcastSlotBreak(
                                            event.player,
                                            EquipmentSlot.HAND,
                                            object : ArrayList<Player>() {
                                                init {
                                                    for (player in Bukkit.getOnlinePlayers()) {
                                                        if (player.world !== event.player.world) continue
                                                        if (player.location
                                                                .distance(event.player.location) < 32
                                                        ) {
                                                            add(player)
                                                        }
                                                    }
                                                }
                                            })
                                        event.player.inventory.setItemInMainHand(ItemStack(Material.AIR))
                                    } else handItem.setItemMeta(damageable)
                                }
                            }
                            if (!blockBreakEvent.isCancelled) {
                                event.getBlock().breakNaturally(event.player.inventory.itemInMainHand, true)
                            }
                            return@Runnable
                        }
                        NMSInvoker.sendBlockDamage(event.player, event.getBlock().location, damage, marker)

                        val target = event.player.getTargetBlockExact(8, FluidCollisionMode.NEVER)
                        if (target == null || target.location != event.getBlock().location) {
                            val taskNum: Int = blockbreakingTasks[event.player]!!
                            cancelTask(taskNum)
                        }
                    } catch (_: NullPointerException) {
                        val taskNum: Int = blockbreakingTasks[event.player]!!
                        cancelTask(taskNum)
                    }
                }, 1, 0)
                if (blockbreakingTasks.containsKey(event.player)) {
                    cancelTask(blockbreakingTasks[event.player]!!)
                    blockbreakingTasks.remove(event.player)
                }
                blockbreakingTasks.put(event.player, task)
                taskEntityMap.put(task, marker)
                taskBlockMap.put(task, event.getBlock())
                taskPlayerMap.put(task, event.player)
            })
        }

        private val taskEntityMap: MutableMap<Int?, Entity?> = HashMap<Int?, Entity?>()
        private val taskPlayerMap: MutableMap<Int?, Player?> = HashMap<Int?, Player?>()
        private val taskBlockMap: MutableMap<Int?, Block?> = HashMap<Int?, Block?>()
        private val blockbreakingTasks: MutableMap<Player?, Int?> = HashMap<Player?, Int?>()

        private fun cancelTask(task: Int) {
            Bukkit.getScheduler().cancelTask(task)
            val marker = taskEntityMap[task]
            val player = taskPlayerMap[task]
            if (player != null && marker != null) {
                NMSInvoker.sendBlockDamage(player, taskBlockMap[task]!!.location, 0f, marker)
                marker.remove()
            }
            taskEntityMap.remove(task)
            taskBlockMap.remove(task)
            taskPlayerMap.remove(task)
        }

        @EventHandler
        fun onBlockDamage(event: OriginsRebornBlockDamageAbortEvent) {
            if (blockbreakingTasks.containsKey(event.player)) {
                val taskNum: Int = blockbreakingTasks[event.player]!!
                cancelTask(taskNum)
            }
        }

        var storedEffects: MutableMap<Player?, SavedPotionEffect> = HashMap<Player?, SavedPotionEffect>()

        @EventHandler
        fun onServerTickEnd(event: ServerTickEndEvent?) {
            val attribute = NMSInvoker.blockBreakSpeedAttribute
            for (player in Bukkit.getOnlinePlayers().toList()) {
                val origins = runBlocking {
                    getOrigins(player)
                }
                val abilities = ArrayList<Ability>()
                for (origin in origins) abilities.addAll(origin.getAbilities())
                var speedModifierAbility: BreakSpeedModifierAbility? = null
                for (ability in abilities) {
                    if (ability is BreakSpeedModifierAbility) {
                        if (ability.shouldActivate(player)) {
                            speedModifierAbility = ability
                            break
                        }
                    }
                }
                if (speedModifierAbility != null) {
                    if (attribute == null) {
                        val effect = player.getPotionEffect(NMSInvoker.miningFatigueEffect)
                        var ambient = false
                        var showParticles = false
                        if (effect != null) {
                            ambient = effect.isAmbient
                            showParticles = effect.hasParticles()
                            if (effect.amplifier != -1) {
                                storedEffects.put(player, SavedPotionEffect(effect, Bukkit.getCurrentTick()))
                                player.removePotionEffect(NMSInvoker.miningFatigueEffect)
                            }
                        }
                        player.addPotionEffect(
                            PotionEffect(
                                NMSInvoker.miningFatigueEffect,
                                infiniteDuration(),
                                -1,
                                ambient,
                                showParticles
                            )
                        )
                    } else {
                        val instance = player.getAttribute(attribute)
                        if (instance == null) continue
                        if (NMSInvoker.getAttributeModifier(instance, key) == null) {
                            NMSInvoker.addAttributeModifier(
                                instance,
                                key,
                                "break-speed-modifier",
                                -1.0,
                                AttributeModifier.Operation.ADD_NUMBER
                            )
                        }
                    }
                } else {
                    if (attribute == null) {
                        if (player.hasPotionEffect(NMSInvoker.miningFatigueEffect)) {
                            val effect = player.getPotionEffect(NMSInvoker.miningFatigueEffect)
                            if (effect != null) {
                                if (effect.amplifier == -1) player.removePotionEffect(NMSInvoker.miningFatigueEffect)
                            }
                        }
                        if (storedEffects.containsKey(player)) {
                            val effect: SavedPotionEffect = storedEffects[player]!!
                            storedEffects.remove(player)
                            val potionEffect: PotionEffect? = checkNotNull(effect.effect)
                            val time = potionEffect!!.duration - (Bukkit.getCurrentTick() - effect.currentTime)
                            if (time > 0) {
                                player.addPotionEffect(
                                    PotionEffect(
                                        potionEffect.type,
                                        time,
                                        potionEffect.amplifier,
                                        potionEffect.isAmbient,
                                        potionEffect.hasParticles()
                                    )
                                )
                            }
                        }
                    } else {
                        val instance = player.getAttribute(attribute)
                        if (instance == null) continue
                        val attributeModifier = NMSInvoker.getAttributeModifier(instance, key)
                        if (attributeModifier == null) continue
                        instance.removeModifier(attributeModifier)
                    }
                }
            }
        }

        companion object {
            private fun getBlockDamage(block: Block, context: BlockMiningContext, time: Int): Float {
                return ((getDestroySpeed(
                    context,
                    block.type
                ) * time * 1000).roundToInt() / 1000).toFloat() / (if (block.getDrops(context.heldItem).isEmpty()) 100 else 30)
            }


            fun getDestroySpeed(context: BlockMiningContext, blockType: Material): Float {
                var f: Float = NMSInvoker.getDestroySpeed(context.heldItem, blockType)

                if (f > 1.0f) {
                    val itemstack = context.heldItem
                    val i = itemstack.getEnchantmentLevel(NMSInvoker.efficiencyEnchantment)

                    if (i > 0 && itemstack.type != Material.AIR) {
                        f += (i * i + 1).toFloat()
                    }
                }

                if (context.hasDigSpeed()) {
                    f *= 1.0f + (context.digSpeedAmplification + 1).toFloat() * 0.2f
                }

                if (context.hasDigSlowdown()) {
                    val digSlowdown = context.digSlowdown
                    val f1: Float = when (digSlowdown) {
                        0 -> 0.3f
                        1 -> 0.09f
                        2 -> 0.0027f
                        else -> 1f
                    }
                    f *= f1
                }

                if (context.underwater && !context.aquaAffinity) {
                    f /= 5.0f
                }

                if (!context.onGround) {
                    f /= 5.0f
                }

                val d = NMSInvoker.getDestroySpeed(blockType)

                return f / d
            }
        }
    }

    companion object {
        private fun isTool(material: Material): Boolean {
            return MaterialTags.PICKAXES.isTagged(material) || MaterialTags.AXES.isTagged(material) || MaterialTags.SWORDS.isTagged(
                material
            ) || MaterialTags.SHOVELS.isTagged(material) || MaterialTags.HOES.isTagged(material) || material == Material.SHEARS || material == Material.TRIDENT
        }

        val key: NamespacedKey = NamespacedKey(instance, "break-speed-modifier")
    }
}
