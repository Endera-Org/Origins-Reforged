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
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.behavior.BehaviorUtils
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.Vec3
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.attribute.AttributeModifier
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlockState
import org.bukkit.craftbukkit.v1_20_R3.entity.*
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack
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
import java.util.function.Function
import java.util.function.Predicate

@Suppress("UnstableApiUsage")
class NMSInvokerV1_20_3 : NMSInvoker() {

    override fun dealExplosionDamage(player: Player, amount: Int) {
        val serverPlayer = (player as CraftPlayer).handle
        serverPlayer.hurt(serverPlayer.damageSources().explosion(null), amount.toFloat())
    }

    override fun dealSonicBoomDamage(entity: LivingEntity, amount: Int, source: Player) {
        val serverPlayer = (source as CraftPlayer).handle
        val e = (entity as CraftEntity).handle
        e.hurt(e.damageSources().sonicBoom(serverPlayer), amount.toFloat())
    }

    override fun getVillagerAfraidGoal(villager: LivingEntity, hasAbility: Predicate<Player>): Goal<Villager> {
        return AvoidEntityGoal(
            (villager as CraftEntity).handle as PathfinderMob,
            net.minecraft.world.entity.player.Player::class.java,
            6f,
            0.5,
            0.8,
            Predicate { livingEntity ->
                val player = livingEntity.bukkitEntity as? Player
                if (player != null) {
                    return@Predicate hasAbility.test(player)
                }
                false
            }
        ).asPaperVanillaGoal()
    }

    override fun getNearestVisiblePlayer(piglin: Piglin): Player {
        val optional = (piglin as CraftPiglin).handle.getBrain()
            .getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER)
        return optional.map(Function { player ->
            player.bukkitEntity as Player
        }).orElse(null)
    }

    override fun throwItem(piglin: Piglin, itemStack: ItemStack, pos: Location) {
        BehaviorUtils.throwItem(
            (piglin as CraftLivingEntity).handle,
            CraftItemStack.asNMSCopy(itemStack),
            Vec3(pos.x, pos.y, pos.z)
        )
    }
    
    
    
    
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

    override fun boostArrow(arrow: Arrow) {
        for (effect in arrow.basePotionType.potionEffects) {
            arrow.addCustomEffect(
                effect.withDuration(effect.duration).withAmplifier(effect.amplifier + 1),
                true
            )
        }
    }

    override val jumpBoostEffect: PotionEffectType
        get() = PotionEffectType.JUMP

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
        get() = Attribute.GENERIC_MAX_ABSORPTION

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

    override fun applyFont(component: Component, font: Key): Component {
        return component.font(font)
    }

    override fun dealDrowningDamage(entity: LivingEntity, amount: Int) {
        val livingEntity = (entity as CraftLivingEntity).handle
        livingEntity.hurt(livingEntity.damageSources().drown(), amount.toFloat())
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

    override fun getDestroySpeed(block: Material): Float {
        return (block.createBlockData().createBlockState() as CraftBlockState).handle.destroySpeed
    }

    override fun getDestroySpeed(item: ItemStack, block: Material): Float {
        val b = (block.createBlockData().createBlockState() as CraftBlockState).handle
        val handle = CraftItemStack.asNMSCopy(item)
        return handle.getDestroySpeed(b)
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
        val packet = ClientboundPlayerInfoUpdatePacket(
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

    override val unbreakingEnchantment: Enchantment
        get() = Enchantment.DURABILITY

    override val efficiencyEnchantment: Enchantment
        get() = Enchantment.DIG_SPEED

    override val respirationEnchantment: Enchantment
        get() = Enchantment.OXYGEN

    override val aquaAffinityEnchantment: Enchantment
        get() = Enchantment.WATER_WORKER

    override val slownessEffect: PotionEffectType
        get() = PotionEffectType.SLOW

    override val baneOfArthropodsEnchantment: Enchantment
        get() = Enchantment.DAMAGE_ARTHROPODS

    override val strengthEffect: PotionEffectType
        get() = PotionEffectType.INCREASE_DAMAGE

    override fun getRespawnLocation(player: Player): Location? {
        return player.bedSpawnLocation
    }

    override fun resetRespawnLocation(player: Player) {
        player.bedSpawnLocation = null
    }

    override fun getAttributeModifier(instance: AttributeInstance, key: NamespacedKey): AttributeModifier? {
        val u = UUID.nameUUIDFromBytes(key.toString().toByteArray())
        return instance.getModifier(u)
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

    override val blockInteractionRangeAttribute: Attribute?
        get() = null

    override val entityInteractionRangeAttribute: Attribute?
        get() = null

    override fun dealDryOutDamage(entity: LivingEntity, amount: Int) {
        val livingEntity = (entity as CraftLivingEntity).handle
        livingEntity.hurt(livingEntity.damageSources().dryOut(), amount.toFloat())
    }

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
