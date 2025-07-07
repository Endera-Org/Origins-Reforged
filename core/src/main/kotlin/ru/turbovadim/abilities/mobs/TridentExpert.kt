package ru.turbovadim.abilities.mobs

import io.papermc.paper.event.player.PlayerStopUsingItemEvent
import net.kyori.adventure.key.Key
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.entity.Trident
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsReforged
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.cooldowns.CooldownAbility
import ru.turbovadim.cooldowns.Cooldowns
import ru.turbovadim.events.PlayerLeftClickEvent
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class TridentExpert : VisibleAbility, Listener, AttributeModifierAbility, CooldownAbility {

    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "You're a master of the trident, dealing +2 damage when you throw it, and +2 melee damage with it. You can also use channeling without thunder, and use riptide without rain/water at the price of extra durability.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "Trident Expert",
            OriginSwapper.LineData.LineComponent.LineType.TITLE
        )

    override val key: Key
        get() = Key.key("moborigins:trident_expert")

    private val riptideKey: NamespacedKey = NamespacedKey(OriginsReforged.instance, "riptide-trident")

    fun fixTrident(item: ItemStack?): ItemStack? {
        if (item == null) return null
        if (item.itemMeta == null) return item
        if (item.itemMeta.persistentDataContainer.has(riptideKey)) {
            val meta = item.itemMeta
            val level = item.itemMeta.persistentDataContainer
                .getOrDefault(riptideKey, PersistentDataType.INTEGER, 1)
            meta.persistentDataContainer.remove(riptideKey)
            meta.addEnchant(Enchantment.RIPTIDE, level, false)
            meta.removeEnchant(Enchantment.FROST_WALKER)
            item.setItemMeta(meta)
        }
        return item
    }

    @EventHandler
    fun onPlayerStopUsingItem(event: PlayerStopUsingItemEvent) {
        if (event.item.type != Material.TRIDENT) return
        if (event.ticksHeldFor >= 10 && event.item.itemMeta.persistentDataContainer
                .has(riptideKey)
        ) {
            releaseUsing(fixTrident(event.item)!!, event.getPlayer().world, event.getPlayer())
            NMSInvoker.damageItem(event.item, 10, event.getPlayer())
        } else fixTrident(event.item)
    }

    @EventHandler
    fun onPlayerLeave(event: PlayerJoinEvent) {
        fixTrident(event.getPlayer().inventory.itemInMainHand)
        fixTrident(event.getPlayer().inventory.itemInOffHand)
    }

    @EventHandler
    fun onPlayerItemHeld(event: PlayerItemHeldEvent) {
        val item = event.getPlayer().inventory.getItem(event.previousSlot)
        if (item != null && item.type == Material.TRIDENT) fixTrident(item)
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.getPlayer().isInWaterOrRainOrBubbleColumn) return
        if (Bukkit.getCurrentTick() - lastTridentEnabledTime.getOrDefault(
                event.getPlayer(),
                Bukkit.getCurrentTick() - 400
            )!! >= 400
        ) return
        if (event.getItem() == null || event.getItem()!!.type != Material.TRIDENT) return
        if (!event.getItem()!!.itemMeta.hasEnchant(Enchantment.RIPTIDE)) return
        val meta = event.getItem()!!.itemMeta
        event.getItem()!!.setItemMeta(meta)
        meta.persistentDataContainer.set<Int?, Int?>(
            riptideKey, PersistentDataType.INTEGER, meta.getEnchantLevel(
                Enchantment.RIPTIDE
            )
        )
        meta.removeEnchant(Enchantment.RIPTIDE)
        if (meta.enchants.isEmpty()) {
            meta.addEnchant(Enchantment.FROST_WALKER, 1, true)
        }
        event.getItem()!!.setItemMeta(meta)
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        event.itemDrop.itemStack = fixTrident(event.itemDrop.itemStack)!!
    }

    private val lastTridentEnabledTime: MutableMap<Player?, Int?> = HashMap<Player?, Int?>()

    @EventHandler
    fun onPlayerLeftClick(event: PlayerLeftClickEvent) {
        if (listOf(Material.AIR, Material.TRIDENT)
                .contains(event.getPlayer().inventory.itemInMainHand.type)
        ) {
            runForAbility(event.getPlayer()) {
                if (hasCooldown(event.getPlayer())) return@runForAbility
                setCooldown(event.getPlayer())
                lastTridentEnabledTime.put(event.getPlayer(), Bukkit.getCurrentTick())
            }
        }
    }

    override val attribute: Attribute
        get() = Attribute.GENERIC_ATTACK_DAMAGE

    override val amount: Double
        get() = 0.0

    override fun getChangedAmount(player: Player): Double {
        return (if (player.inventory.itemInMainHand
                .type == Material.TRIDENT && Bukkit.getCurrentTick() - lastTridentEnabledTime.getOrDefault(
                player,
                Bukkit.getCurrentTick() - 400
            )!! < 400
        ) 2 else 0).toDouble()
    }

    @EventHandler
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val player = event.getEntity().shooter as? Player
        if (player != null) {
            val trident = event.getEntity() as? Trident
            if (trident != null) {
                runForAbility(player) {
                        if (Bukkit.getCurrentTick() - lastTridentEnabledTime.getOrDefault(
                                player,
                                Bukkit.getCurrentTick() - 400
                            )!! < 400
                        ) {
                            trident.damage = trident.damage + 2
                        }
                    }
            }
        }
    }

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        if (event.hitEntity == null) return
        val player = event.getEntity().shooter as? Player
        val trident = event.getEntity() as? Trident
        if (player != null && trident != null) {
            runForAbility(player) {
                    if (Bukkit.getCurrentTick() - lastTridentEnabledTime.getOrDefault(
                            player,
                            Bukkit.getCurrentTick() - 400
                        )!! < 400
                    ) {
                        if (trident.itemStack.itemMeta.hasEnchant(Enchantment.CHANNELING)) {
                            event.hitEntity!!.world.strikeLightning(event.hitEntity!!.location)
                        }
                    }
                }
        }
    }

    override val operation: AttributeModifier.Operation
        get() = AttributeModifier.Operation.ADD_NUMBER

    fun releaseUsing(stack: ItemStack, world: World, player: Player) {
        val k = stack.getEnchantmentLevel(Enchantment.RIPTIDE)

        if (k > 0) {
            val event = PlayerRiptideEvent(player, stack)
            event.getPlayer().server.pluginManager.callEvent(event)
            val f = player.location.yaw
            val f1 = player.location.pitch
            var f2 = -sin((f * 0.017453292f).toDouble()) * cos((f1 * 0.017453292f).toDouble())
            var f3 = -sin((f1 * 0.017453292f).toDouble())
            var f4 = cos((f * 0.017453292f).toDouble()) * cos((f1 * 0.017453292f).toDouble())
            val f5 = sqrt(f2 * f2 + f3 * f3 + f4 * f4)
            val f6 = 3.0f * ((1.0f + k.toFloat()) / 4.0f)

            f2 *= f6 / f5
            f3 *= f6 / f5
            f4 *= f6 / f5
            if (player.isOnGround) {
                NMSInvoker.tridentMove(player)
            }
            player.velocity = player.velocity.add(Vector(f2, f3, f4))
            NMSInvoker.startAutoSpinAttack(player, 20, 8.0f, stack)

            val soundeffect = if (k >= 3) {
                Sound.ITEM_TRIDENT_RIPTIDE_3
            } else if (k == 2) {
                Sound.ITEM_TRIDENT_RIPTIDE_2
            } else {
                Sound.ITEM_TRIDENT_RIPTIDE_1
            }

            world.playSound(player.location, soundeffect, 1f, 1f)
        }
    }

    override val cooldownInfo: Cooldowns.CooldownInfo
        get() = Cooldowns.CooldownInfo(400, "trident_expert", true)
}
