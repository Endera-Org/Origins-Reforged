package ru.turbovadim.packetsenders

import com.destroystokyo.paper.entity.ai.Goal
import io.netty.buffer.Unpooled
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.TriState
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal
import net.minecraft.world.level.GameType
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.attribute.AttributeModifier
import org.bukkit.craftbukkit.v1_18_R2.block.data.CraftBlockData
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftEntity
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftLivingEntity
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Creeper
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockDamageAbortEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.potion.PotionEffectType
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Predicate

class NMSInvokerV1_18_2 : NMSInvoker() {
    
    override fun setCustomModelData(meta: ItemMeta, cmd: Int): ItemMeta {
        meta.setCustomModelData(cmd)
        return meta
    }

    override val miningEfficiencyAttribute: Attribute?
        get() = null

    override val sneakingSpeedAttribute: Attribute?
        get() = null

    override val submergedMiningSpeedAttribute: Attribute?
        get() = null

    override val sweepingDamageRatioAttribute: Attribute?
        get() = null

    override val flyingSpeedAttribute: Attribute
        get() = Attribute.GENERIC_FLYING_SPEED

    override val attackKnockbackAttribute: Attribute
        get() = Attribute.GENERIC_ATTACK_KNOCKBACK

    override val attackSpeedAttribute: Attribute
        get() = Attribute.GENERIC_ATTACK_SPEED

    override val armorToughnessAttribute: Attribute
        get() = Attribute.GENERIC_ARMOR_TOUGHNESS

    override val luckAttribute: Attribute
        get() = Attribute.GENERIC_LUCK

    override val horseJumpStrengthAttribute: Attribute
        get() = Attribute.HORSE_JUMP_STRENGTH

    override val spawnReinforcementsAttribute: Attribute
        get() = Attribute.ZOMBIE_SPAWN_REINFORCEMENTS

    override val followRangeAttribute: Attribute
        get() = Attribute.GENERIC_FOLLOW_RANGE

    override val knockbackResistanceAttribute: Attribute
        get() = Attribute.GENERIC_KNOCKBACK_RESISTANCE

    override val fallDamageMultiplierAttribute: Attribute?
        get() = null

    override val maxAbsorptionAttribute: Attribute?
        get() = null

    override val safeFallDistanceAttribute: Attribute?
        get() = null

    override val scaleAttribute: Attribute?
        get() = null

    override val stepHeightAttribute: Attribute?
        get() = null

    override val gravityAttribute: Attribute?
        get() = null

    override val jumpStrengthAttribute: Attribute?
        get() = null

    override val burningTimeAttribute: Attribute?
        get() = null

    override val explosionKnockbackResistanceAttribute: Attribute?
        get() = null

    override val movementEfficiencyAttribute: Attribute?
        get() = null

    override val oxygenBonusAttribute: Attribute?
        get() = null

    override val waterMovementEfficiencyAttribute: Attribute?
        get() = null

    override val temptRangeAttribute: Attribute?
        get() = null

    @EventHandler
    fun onBlockDamageAbort(event: BlockDamageAbortEvent) {
        OriginsRebornBlockDamageAbortEvent(event.player, event.getBlock(), event.itemInHand).callEvent()
    }

    override fun applyFont(component: Component, font: Key): Component {
        return component.font(font)
    }

    override val armorAttribute: Attribute
        get() = Attribute.GENERIC_ARMOR

    override val maxHealthAttribute: Attribute
        get() = Attribute.GENERIC_MAX_HEALTH

    override val movementSpeedAttribute: Attribute
        get() = Attribute.GENERIC_MOVEMENT_SPEED

    override val attackDamageAttribute: Attribute
        get() = Attribute.GENERIC_ATTACK_DAMAGE

    override fun sendEntityData(player: Player, entity: Entity, bytes: Byte) {
        val serverPlayer = (player as CraftPlayer).handle
        val data = (entity as CraftEntity).handle.getEntityData()
        data.set<Byte?>(EntityDataAccessor<Byte?>(0, EntityDataSerializers.BYTE), bytes)
        val packet = ClientboundSetEntityDataPacket(entity.entityId, data, false)
        serverPlayer.connection.send(packet)
    }

    override fun getCreeperAfraidGoal(
        creeper: LivingEntity,
        hasAbility: Predicate<Player>,
        hasKey: Predicate<LivingEntity>
    ): Goal<Creeper> {
        return AvoidEntityGoal<net.minecraft.world.entity.player.Player>(
            (creeper as CraftEntity).handle as PathfinderMob,
            net.minecraft.world.entity.player.Player::class.java,
            6f,
            1.0,
            1.2,
            Predicate { livingEntity: net.minecraft.world.entity.LivingEntity? ->
                val player = livingEntity!!.bukkitEntity as? Player
                if (player != null) {
                    if (hasAbility.test(player)) {
                        return@Predicate (!hasKey.test(creeper))
                    }
                }
                false
            }

        ).asPaperVanillaGoal<Creeper>()
    }

    override fun wasTouchingWater(player: Player): Boolean {
        return (player as CraftPlayer).handle.wasTouchingWater
    }

    override fun getDestroySpeed(item: ItemStack, block: Material): Float {
        val b = (block.createBlockData() as CraftBlockData).state
        val handle = CraftItemStack.asNMSCopy(item)
        return handle.getDestroySpeed(b)
    }

    override fun getDestroySpeed(block: Material): Float {
        return (block.createBlockData() as CraftBlockData).state.destroySpeed
    }

    override fun setNoPhysics(player: Player, noPhysics: Boolean) {
        (player as CraftPlayer).handle.noPhysics = noPhysics
    }

    override fun sendPhasingGamemodeUpdate(player: Player, gameMode: GameMode) {
        val serverPlayer = (player as CraftPlayer).handle

        val gameType = when (gameMode) {
            GameMode.CREATIVE -> GameType.CREATIVE
            GameMode.SURVIVAL -> GameType.SURVIVAL
            GameMode.ADVENTURE -> GameType.ADVENTURE
            GameMode.SPECTATOR -> GameType.SPECTATOR
        }

        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeEnum(ClientboundPlayerInfoPacket.Action.UPDATE_GAME_MODE)
        buf.writeCollection<String?>(mutableListOf<String?>("null"), BiConsumer { buf2: FriendlyByteBuf?, e: String? ->
            buf2!!.writeUUID(serverPlayer.getUUID())
            buf2.writeVarInt(gameType.id)
        })

        val packet = ClientboundPlayerInfoPacket(buf)

        serverPlayer.connection.send(packet)
    }

    override fun sendResourcePacks(
        player: Player,
        pack: String,
        extraPacks: MutableMap<*, OriginsRebornResourcePackInfo>
    ) {
        player.setResourcePack(pack)
    }

    override val nauseaEffect: PotionEffectType
        get() = PotionEffectType.CONFUSION

    override val miningFatigueEffect: PotionEffectType
        get() = PotionEffectType.SLOW_DIGGING

    override val hasteEffect: PotionEffectType
        get() = PotionEffectType.FAST_DIGGING

    override val jumpBoostEffect: PotionEffectType
        get() = PotionEffectType.JUMP

    override val slownessEffect: PotionEffectType
        get() = PotionEffectType.SLOW

    override val strengthEffect: PotionEffectType
        get() = PotionEffectType.INCREASE_DAMAGE

    override val unbreakingEnchantment: Enchantment
        get() = Enchantment.DURABILITY

    override val aquaAffinityEnchantment: Enchantment
        get() = Enchantment.WATER_WORKER

    override val baneOfArthropodsEnchantment: Enchantment
        get() = Enchantment.DAMAGE_ARTHROPODS

    override val efficiencyEnchantment: Enchantment
        get() = Enchantment.DIG_SPEED

    override val respirationEnchantment: Enchantment
        get() = Enchantment.OXYGEN

    override fun getRespawnLocation(player: Player): Location? {
        return player.bedSpawnLocation
    }

    override fun resetRespawnLocation(player: Player) {
        player.bedSpawnLocation = null
    }

    override fun getAttributeModifier(instance: AttributeInstance, key: NamespacedKey): AttributeModifier? {
        val u = UUID.nameUUIDFromBytes(key.toString().toByteArray())
        for (am in instance.modifiers) {
            if (am.uniqueId == u) return am
        }
        return null
    }

    override fun addAttributeModifier(
        instance: AttributeInstance,
        key: NamespacedKey,
        name: String,
        amount: Double,
        operation: AttributeModifier.Operation
    ) {
        instance.addModifier(
            AttributeModifier(
                UUID.nameUUIDFromBytes(key.toString().toByteArray()),
                name,
                amount,
                operation
            )
        )
    }

    override fun setWorldBorderOverlay(player: Player, show: Boolean) {
        if (show) {
            val border = Bukkit.createWorldBorder()
            border.center = player.world.worldBorder.center
            border.size = player.world.worldBorder.size
            border.warningDistance = (player.world.worldBorder.size * 2).toInt()
            player.worldBorder = border
        } else player.worldBorder = null
    }

    override fun dealDryOutDamage(entity: LivingEntity, amount: Int) {
        val livingEntity = (entity as CraftLivingEntity).handle
        livingEntity.hurt(DamageSource.DRY_OUT, amount.toFloat())
    }

    override fun dealDrowningDamage(entity: LivingEntity, amount: Int) {
        val livingEntity = (entity as CraftLivingEntity).handle
        livingEntity.hurt(DamageSource.DROWN, amount.toFloat())
    }

    override fun dealFreezeDamage(entity: LivingEntity, amount: Int) {
        val livingEntity = (entity as CraftLivingEntity).handle
        livingEntity.hurt(DamageSource.FREEZE, amount.toFloat())
    }

    override fun supportsInfiniteDuration(): Boolean {
        return false
    }

    override fun isUnderWater(entity: LivingEntity): Boolean {
        return (entity as CraftLivingEntity).handle.isUnderWater
    }

    override val blockInteractionRangeAttribute: Attribute?
        get() = null

    override val entityInteractionRangeAttribute: Attribute?
        get() = null

    override fun knockback(entity: LivingEntity, strength: Double, x: Double, z: Double) {
        (entity as CraftLivingEntity).handle.knockback(strength, x, z)
    }

    override fun broadcastSlotBreak(player: Player, slot: EquipmentSlot, players: MutableCollection<Player>) {
        val nmsSlot = when (slot) {
            EquipmentSlot.HAND -> net.minecraft.world.entity.EquipmentSlot.MAINHAND
            EquipmentSlot.OFF_HAND -> net.minecraft.world.entity.EquipmentSlot.OFFHAND
            EquipmentSlot.FEET -> net.minecraft.world.entity.EquipmentSlot.FEET
            EquipmentSlot.LEGS -> net.minecraft.world.entity.EquipmentSlot.LEGS
            EquipmentSlot.CHEST -> net.minecraft.world.entity.EquipmentSlot.CHEST
            EquipmentSlot.HEAD -> net.minecraft.world.entity.EquipmentSlot.HEAD
        }
        (player as CraftPlayer).handle.broadcastBreakEvent(nmsSlot)
    }

    override fun sendBlockDamage(player: Player, location: Location, damage: Float, entity: Entity) {
        val packet = ClientboundBlockDestructionPacket(
            entity.entityId,
            BlockPos(location.x, location.y, location.z),
            (damage * 10).toInt()
        )
        (player as CraftPlayer).handle.connection.send(packet)
    }

    override fun setFlyingFallDamage(player: Player, state: TriState) {
        flyingFallDamage.put(player, state)
    }

    var flyingFallDamage: MutableMap<Player?, TriState?> = HashMap<Player?, TriState?>()

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player
        if (player != null) {
            if (player.allowFlight) {
                if (flyingFallDamage[player] == TriState.FALSE) event.isCancelled = true
            }
        }
    }

    override val blockBreakSpeedAttribute: Attribute?
        get() = null
}