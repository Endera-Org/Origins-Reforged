package ru.turbovadim.packetsenders

import com.destroystokyo.paper.entity.ai.Goal
import net.kyori.adventure.key.Key
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.TriState
import net.minecraft.Optionull
import net.minecraft.network.chat.RemoteChatSession
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal
import net.minecraft.world.level.GameType
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.attribute.AttributeModifier
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.block.CraftBlockState
import org.bukkit.craftbukkit.entity.CraftAllay
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockDamageAbortEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.potion.PotionEffectType
import java.net.URI
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.function.Function
import java.util.function.Predicate

class NMSInvokerV1_21_3 : NMSInvoker() {

    override fun duplicateAllay(allay: Allay): Boolean {
        if (allay.duplicationCooldown > 0) return false
        allay.duplicateAllay()
        (allay.world as CraftWorld).handle
            .broadcastEntityEvent((allay as CraftAllay).handle, 18.toByte())
        return true
    }

    override fun applyFont(component: Component, font: Key): Component {
        return component.font(font)
    }

    override fun sendEntityData(player: Player, entity: Entity, bytes: Byte) {
        val serverPlayer = (player as CraftPlayer).handle
        val target = (entity as CraftEntity).handle

        val eData: MutableList<SynchedEntityData.DataValue<*>?> = ArrayList<SynchedEntityData.DataValue<*>?>()
        eData.add(
            SynchedEntityData.DataValue.create<Byte?>(
                EntityDataAccessor<Byte?>(0, EntityDataSerializers.BYTE),
                bytes
            )
        )
        val metadata = ClientboundSetEntityDataPacket(target.id, eData)
        serverPlayer.connection.send(metadata)
    }

    override fun setCustomModelData(meta: ItemMeta, cmd: Int): ItemMeta {
        meta.setCustomModelData(cmd)
        return meta
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
            Predicate { livingEntity: net.minecraft.world.entity.LivingEntity ->
                val player = livingEntity.bukkitEntity as? Player
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

    override fun getDestroySpeed(block: Material): Float {
        return (block.createBlockData().createBlockState() as CraftBlockState).handle.destroySpeed
    }

    override fun getDestroySpeed(item: ItemStack, block: Material): Float {
        val b = (block.createBlockData().createBlockState() as CraftBlockState).handle
        val handle = CraftItemStack.asNMSCopy(item)
        return handle.getDestroySpeed(b)
    }

    override val armorAttribute: Attribute
        get() = Attribute.ARMOR

    override val maxHealthAttribute: Attribute
        get() = Attribute.MAX_HEALTH

    override val movementSpeedAttribute: Attribute
        get() = Attribute.MOVEMENT_SPEED

    override val attackDamageAttribute: Attribute
        get() = Attribute.ATTACK_DAMAGE

    override val flyingSpeedAttribute: Attribute
        get() = Attribute.FLYING_SPEED

    override val attackKnockbackAttribute: Attribute
        get() = Attribute.ATTACK_KNOCKBACK

    override val attackSpeedAttribute: Attribute
        get() = Attribute.ATTACK_SPEED

    override val armorToughnessAttribute: Attribute
        get() = Attribute.ARMOR_TOUGHNESS

    override val luckAttribute: Attribute
        get() = Attribute.LUCK

    override val horseJumpStrengthAttribute: Attribute
        get() = Attribute.JUMP_STRENGTH

    override val spawnReinforcementsAttribute: Attribute
        get() = Attribute.SPAWN_REINFORCEMENTS

    override val followRangeAttribute: Attribute
        get() = Attribute.FOLLOW_RANGE

    override val knockbackResistanceAttribute: Attribute
        get() = Attribute.KNOCKBACK_RESISTANCE

    override val fallDamageMultiplierAttribute: Attribute?
        get() = Attribute.FALL_DAMAGE_MULTIPLIER

    override val maxAbsorptionAttribute: Attribute?
        get() = Attribute.MAX_ABSORPTION

    override val safeFallDistanceAttribute: Attribute?
        get() = Attribute.SAFE_FALL_DISTANCE

    override val scaleAttribute: Attribute?
        get() = Attribute.SCALE

    override val stepHeightAttribute: Attribute?
        get() = Attribute.STEP_HEIGHT

    override val gravityAttribute: Attribute?
        get() = Attribute.GRAVITY

    override val jumpStrengthAttribute: Attribute?
        get() = Attribute.JUMP_STRENGTH

    override val burningTimeAttribute: Attribute?
        get() = Attribute.BURNING_TIME

    override val explosionKnockbackResistanceAttribute: Attribute?
        get() = Attribute.EXPLOSION_KNOCKBACK_RESISTANCE

    override val movementEfficiencyAttribute: Attribute?
        get() = Attribute.MOVEMENT_EFFICIENCY

    override val oxygenBonusAttribute: Attribute?
        get() = Attribute.OXYGEN_BONUS

    override val waterMovementEfficiencyAttribute: Attribute?
        get() = Attribute.WATER_MOVEMENT_EFFICIENCY

    override val temptRangeAttribute: Attribute?
        get() = Attribute.TEMPT_RANGE

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
        val entry = ClientboundPlayerInfoUpdatePacket.Entry(
            serverPlayer.getUUID(),
            serverPlayer.getGameProfile(),
            true,
            1,
            gameType,
            serverPlayer.tabListDisplayName,
            0,
            Optionull.map<RemoteChatSession?, RemoteChatSession.Data?>(
                serverPlayer.chatSession,
                Function { obj: RemoteChatSession? -> obj!!.asData() })
        )
        val packet = ClientboundPlayerInfoUpdatePacket(
            EnumSet.of<ClientboundPlayerInfoUpdatePacket.Action?>(
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE
            ), entry
        )
        serverPlayer.connection.send(packet)
    }

    override fun sendResourcePacks(
        player: Player,
        pack: String,
        extraPacks: MutableMap<*, OriginsRebornResourcePackInfo>
    ) {
        try {
            val packInfo = ResourcePackInfo.resourcePackInfo()
                .uri(URI.create(pack))
                .computeHashAndBuild().get()
            val packs: MutableList<ResourcePackInfo?> = ArrayList<ResourcePackInfo?>()
            packs.add(packInfo)
            for (originsRebornResourcePackInfo in extraPacks.values) {
                val info = originsRebornResourcePackInfo.packInfo as? ResourcePackInfo
                if (info != null) {
                    packs.add(info)
                }
            }
            player.sendResourcePacks(
                ResourcePackRequest.resourcePackRequest()
                    .packs(packs)
                    .required(true)
                    .build()
            )
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        } catch (e: ExecutionException) {
            throw RuntimeException(e)
        }
    }

    override val nauseaEffect: PotionEffectType
        get() = PotionEffectType.NAUSEA

    override val miningFatigueEffect: PotionEffectType
        get() = PotionEffectType.MINING_FATIGUE

    override val hasteEffect: PotionEffectType
        get() = PotionEffectType.HASTE

    override val unbreakingEnchantment: Enchantment
        get() = Enchantment.UNBREAKING

    override val efficiencyEnchantment: Enchantment
        get() = Enchantment.EFFICIENCY

    override val respirationEnchantment: Enchantment
        get() = Enchantment.RESPIRATION

    override val aquaAffinityEnchantment: Enchantment
        get() = Enchantment.AQUA_AFFINITY

    override val jumpBoostEffect: PotionEffectType
        get() = PotionEffectType.JUMP_BOOST

    override val slownessEffect: PotionEffectType
        get() = PotionEffectType.SLOWNESS

    override val baneOfArthropodsEnchantment: Enchantment
        get() = Enchantment.BANE_OF_ARTHROPODS

    override fun getRespawnLocation(player: Player): Location? {
        return player.respawnLocation
    }

    override fun resetRespawnLocation(player: Player) {
        player.respawnLocation = null
    }

    override fun getAttributeModifier(instance: AttributeInstance, key: NamespacedKey): AttributeModifier? {
        return instance.getModifier(key)
    }

    override fun addAttributeModifier(
        instance: AttributeInstance,
        key: NamespacedKey,
        name: String,
        amount: Double,
        operation: AttributeModifier.Operation
    ) {
        instance.addModifier(AttributeModifier(key, amount, operation, EquipmentSlotGroup.ANY))
    }

    override val strengthEffect: PotionEffectType
        get() = PotionEffectType.STRENGTH

    override fun dealDryOutDamage(entity: LivingEntity, amount: Int) {
        entity.damage(amount.toDouble(), DamageSource.builder(DamageType.DRY_OUT).build())
    }

    override fun dealFreezeDamage(entity: LivingEntity, amount: Int) {
        entity.damage(amount.toDouble(), DamageSource.builder(DamageType.FREEZE).build())
    }

    override fun isUnderWater(entity: LivingEntity): Boolean {
        return entity.isUnderWater
    }

    override fun knockback(entity: LivingEntity, strength: Double, x: Double, z: Double) {
        entity.knockback(strength, x, z)
    }

    override fun setFlyingFallDamage(player: Player, state: TriState) {
        player.setFlyingFallDamage(state)
    }

    override fun broadcastSlotBreak(player: Player, slot: EquipmentSlot, players: MutableCollection<Player>) {
        player.broadcastSlotBreak(slot, players)
    }

    override fun sendBlockDamage(player: Player, location: Location, damage: Float, entity: Entity) {
        player.sendBlockDamage(location, damage, entity)
    }

    override val blockInteractionRangeAttribute: Attribute?
        get() = Attribute.BLOCK_INTERACTION_RANGE

    override val entityInteractionRangeAttribute: Attribute?
        get() = Attribute.ENTITY_INTERACTION_RANGE

    override val blockBreakSpeedAttribute: Attribute?
        get() = Attribute.BLOCK_BREAK_SPEED

    override val miningEfficiencyAttribute: Attribute?
        get() = Attribute.MINING_EFFICIENCY

    override val sneakingSpeedAttribute: Attribute?
        get() = Attribute.SNEAKING_SPEED

    override val submergedMiningSpeedAttribute: Attribute?
        get() = Attribute.SUBMERGED_MINING_SPEED

    override val sweepingDamageRatioAttribute: Attribute?
        get() = Attribute.SWEEPING_DAMAGE_RATIO

    override fun setWorldBorderOverlay(player: Player, show: Boolean) {
        if (show) {
            val border = Bukkit.createWorldBorder()
            border.center = player.world.worldBorder.center
            border.size = player.world.worldBorder.size
            border.warningDistance = (player.world.worldBorder.size * 2).toInt()
            player.worldBorder = border
        } else player.worldBorder = null
    }

    override fun dealDrowningDamage(entity: LivingEntity, amount: Int) {
        entity.damage(amount.toDouble(), DamageSource.builder(DamageType.DROWN).build())
    }

    @EventHandler
    fun onBlockDamageAbort(event: BlockDamageAbortEvent) {
        OriginsRebornBlockDamageAbortEvent(event.player, event.getBlock(), event.itemInHand).callEvent()
    }

    override val ominousBottle: Material?
        get() = Material.OMINOUS_BOTTLE
}
