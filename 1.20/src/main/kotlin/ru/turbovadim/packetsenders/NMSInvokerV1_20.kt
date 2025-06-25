package ru.turbovadim.packetsenders

import com.destroystokyo.paper.entity.ai.Goal
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.TriState
import net.minecraft.Optionull
import net.minecraft.network.chat.RemoteChatSession
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.projectile.Arrow
import net.minecraft.world.item.alchemy.PotionUtils
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.Vec3
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.attribute.AttributeModifier
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld
import org.bukkit.craftbukkit.v1_20_R1.block.data.CraftBlockData
import org.bukkit.craftbukkit.v1_20_R1.entity.*
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockDamageAbortEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import org.spigotmc.event.entity.EntityDismountEvent
import org.spigotmc.event.entity.EntityMountEvent
import java.util.*
import java.util.function.Predicate

class NMSInvokerV1_20 : NMSInvoker() {

    override fun dealThornsDamage(target: Entity, amount: Int, attacker: Entity) {
        val entity = (target as CraftEntity).handle
        entity.hurt(entity.damageSources().thorns((attacker as CraftEntity).handle), amount.toFloat())
    }

    override fun getSmiteEnchantment(): Enchantment {
        return Enchantment.DAMAGE_UNDEAD
    }

    override fun getElderGuardianParticle(): Particle {
        return Particle.MOB_APPEARANCE
    }

    override fun getWitchParticle(): Particle {
        return Particle.SPELL_WITCH
    }

    override fun damageItem(item: ItemStack, amount: Int, player: Player) {
        item.damage(amount, player)
    }

    override fun startAutoSpinAttack(
        player: Player,
        duration: Int,
        riptideAttackDamage: Float,
        item: ItemStack
    ) {
        (player as CraftPlayer).handle.startAutoSpinAttack(duration)
    }

    override fun tridentMove(player: Player) {
        (player as CraftPlayer).handle.move(MoverType.SELF, Vec3(0.0, 1.1999999284744263, 0.0))
    }

    override fun getIronGolemAttackGoal(golem: LivingEntity, hasAbility: Predicate<Player>): Goal<Mob> {
        return NearestAttackableTargetGoal(
            (golem as CraftMob).handle,
            net.minecraft.world.entity.player.Player::class.java,
            10,
            true,
            false,
            Predicate { livingEntity ->
                val player = livingEntity.bukkitEntity as? Player
                if (player != null) {
                    return@Predicate hasAbility.test(player)
                } else return@Predicate false
            }).asPaperVanillaGoal<Mob>()
    }

    private val lastVec3Map: MutableMap<Player?, Vec3?> = HashMap<Player?, Vec3?>()

    override fun bounce(player: Player) {
        val p = (player as CraftPlayer).handle
        if (player.isOnGround) {
            if (player.fallDistance <= 0) return
            val dm = lastVec3Map[player]
            if (dm != null) {
                player.velocity = player.velocity.add(Vector(0.0, -dm.y, 0.0))
            }
        }
        lastVec3Map.put(player, p.deltaMovement)
    }





    override fun getGenericScaleAttribute(): Attribute? {
        return null
    }

    override fun transferDamageEvent(entity: LivingEntity, event: EntityDamageEvent) {
        entity.damage(event.damage)
    }

    override fun getGenericJumpStrengthAttribute(): Attribute {
        return Attribute.HORSE_JUMP_STRENGTH
    }

    @EventHandler
    fun onEntityMount(event: EntityMountEvent) {
        event.isCancelled = !FantasyEntityMountEvent(event.getEntity(), event.mount).callEvent()
    }

    @EventHandler
    fun onEntityDismount(event: EntityDismountEvent) {
        event.isCancelled = !FantasyEntityDismountEvent(
            event.getEntity(),
            event.dismounted,
            event.isCancellable
        ).callEvent()
    }

    override fun getFortuneEnchantment(): Enchantment = Enchantment.LOOT_BONUS_BLOCKS

    override fun launchArrow(projectile: Entity, entity: Entity, roll: Float, force: Float, divergence: Float) {
        (projectile as AbstractProjectile).handle.shootFromRotation(
            (entity as CraftEntity).handle,
            entity.location.pitch,
            entity.location.yaw,
            roll,
            force,
            divergence
        )
    }

    override fun boostArrow(arrow: org.bukkit.entity.Arrow) {
        val a = (arrow as CraftArrow).handle
        if (a is Arrow) {
            for (instance in PotionUtils.getPotion(a.pickupItem).effects) {
                a.addEffect(
                    MobEffectInstance(
                        instance.effect,
                        instance.duration,
                        instance.amplifier + 1
                    )
                )
            }
        }
    }


    override fun duplicateAllay(allay: Allay): Boolean {
        if (allay.duplicationCooldown > 0) return false
        allay.duplicateAllay()
        (allay.world as CraftWorld).handle
            .broadcastEntityEvent((allay as CraftAllay).handle, 18.toByte())
        return true
    }

    override val miningEfficiencyAttribute: Attribute?
        get() = null

    override val sneakingSpeedAttribute: Attribute?
        get() = null

    override val submergedMiningSpeedAttribute: Attribute?
        get() = null

    override val sweepingDamageRatioAttribute: Attribute?
        get() = null

    override fun setCustomModelData(meta: ItemMeta, cmd: Int): ItemMeta {
        meta.setCustomModelData(cmd)
        return meta
    }

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

    override fun dealDrowningDamage(entity: LivingEntity, amount: Int) {
        val livingEntity = (entity as CraftLivingEntity).handle
        livingEntity.hurt(livingEntity.damageSources().drown(), amount.toFloat())
    }

    override val respirationEnchantment: Enchantment
        get() = Enchantment.OXYGEN

    override fun applyFont(component: Component, font: Key): Component {
        return component.font(font)
    }

    @EventHandler
    fun onBlockDamageAbort(event: BlockDamageAbortEvent) {
        OriginsReforgedBlockDamageAbortEvent(event.player, event.getBlock(), event.itemInHand).callEvent()
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
        val target = (entity as CraftEntity).handle

        val eData: MutableList<SynchedEntityData.DataValue<*>?> = ArrayList<SynchedEntityData.DataValue<*>?>()
        eData.add(
            SynchedEntityData.DataValue.create(
                EntityDataAccessor(0, EntityDataSerializers.BYTE),
                bytes
            )
        )
        val metadata = ClientboundSetEntityDataPacket(target.id, eData)
        serverPlayer.connection.send(metadata)
    }

    override fun getCreeperAfraidGoal(
        creeper: LivingEntity,
        hasAbility: Predicate<Player>,
        hasKey: Predicate<LivingEntity>
    ): Goal<Creeper> {
        return AvoidEntityGoal(
            (creeper as CraftEntity).handle as PathfinderMob,
            net.minecraft.world.entity.player.Player::class.java,
            6f,
            1.0,
            1.2,
            Predicate { livingEntity: net.minecraft.world.entity.LivingEntity? ->
                val player = livingEntity?.bukkitEntity as? Player
                if (player != null) {
                    if (hasAbility.test(player)) {
                        return@Predicate (!hasKey.test(creeper))
                    }
                }
                false
            }

        ).asPaperVanillaGoal()
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
        val entry = ClientboundPlayerInfoUpdatePacket.Entry(
            serverPlayer.getUUID(),
            serverPlayer.getGameProfile(),
            true,
            1,
            gameType,
            serverPlayer.tabListDisplayName,
            Optionull.map(
                serverPlayer.chatSession
            ) { obj: RemoteChatSession? -> obj!!.asData() }
        )
        val packet: ClientboundPlayerInfoUpdatePacket = OriginsReforgedClientboundPlayerInfoUpdatePacket(
            EnumSet.of(
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE
            ), entry
        )
        serverPlayer.connection.send(packet)
    }

    override fun sendResourcePacks(
        player: Player,
        pack: String,
        extraPacks: MutableMap<*, OriginsReforgedResourcePackInfo>
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

    class OriginsReforgedClientboundPlayerInfoUpdatePacket(actions: EnumSet<Action?>, entry: Entry?) :
        ClientboundPlayerInfoUpdatePacket(actions, mutableListOf<ServerPlayer?>()) {
        init {
            entries().add(entry)
        }
    }

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

    override fun isUnderWater(entity: LivingEntity): Boolean {
        return entity.isUnderWater
    }

    override fun knockback(entity: LivingEntity, strength: Double, x: Double, z: Double) {
        entity.knockback(strength, x, z)
    }

    override fun dealDryOutDamage(entity: LivingEntity, amount: Int) {
        val livingEntity = (entity as CraftLivingEntity).handle
        livingEntity.hurt(livingEntity.damageSources().dryOut(), amount.toFloat())
    }

    override val blockInteractionRangeAttribute: Attribute?
        get() = null

    override val entityInteractionRangeAttribute: Attribute?
        get() = null

    override fun dealFreezeDamage(entity: LivingEntity, amount: Int) {
        val livingEntity = (entity as CraftLivingEntity).handle
        livingEntity.hurt(livingEntity.damageSources().freeze(), amount.toFloat())
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

    override val blockBreakSpeedAttribute: Attribute?
        get() = null

    override fun setWorldBorderOverlay(player: Player, show: Boolean) {
        if (show) {
            val border = Bukkit.createWorldBorder()
            border.center = player.world.worldBorder.center
            border.size = player.world.worldBorder.size
            border.warningDistance = (player.world.worldBorder.size * 2).toInt()
            player.worldBorder = border
        } else player.worldBorder = null
    }
}
