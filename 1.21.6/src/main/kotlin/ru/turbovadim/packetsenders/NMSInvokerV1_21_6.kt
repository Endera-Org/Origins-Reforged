package ru.turbovadim.packetsenders

import com.destroystokyo.paper.entity.ai.Goal
import net.kyori.adventure.key.Key
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.TriState
import net.minecraft.Optionull
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.behavior.BehaviorUtils
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.ai.targeting.TargetingConditions
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.Vec3
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.attribute.AttributeModifier
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.block.CraftBlockState
import org.bukkit.craftbukkit.entity.*
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockDamageAbortEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.entity.EntityMountEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.net.URI
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.function.Function
import java.util.function.Predicate

@Suppress("UnstableApiUsage")
class NMSInvokerV1_21_6 : NMSInvoker() {

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
        ).asPaperGoal()
    }

    override fun getNearestVisiblePlayer(piglin: Piglin): Player {
        val optional = (piglin as CraftPiglin).handle.getBrain()
            .getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER)
        return optional.map(Function { player -> player.bukkitEntity as Player })
            .orElse(null)
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
        entity.hurtServer(
            entity.level() as ServerLevel,
            entity.damageSources().thorns((attacker as CraftEntity).handle),
            amount.toFloat()
        )
    }

    override fun getSmiteEnchantment(): Enchantment {
        return Enchantment.SMITE
    }

    override fun getElderGuardianParticle(): Particle {
        return Particle.ELDER_GUARDIAN
    }

    override fun getWitchParticle(): Particle {
        return Particle.WITCH
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
        (player as CraftPlayer).handle
            .startAutoSpinAttack(duration, riptideAttackDamage, CraftItemStack.asNMSCopy(item))
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
            TargetingConditions.Selector { livingEntity: net.minecraft.world.entity.LivingEntity, serverLevel ->
                val player = livingEntity.bukkitEntity as? Player
                if (player != null) {
                    return@Selector hasAbility.test(player)
                } else return@Selector false
            }).asPaperGoal()
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


    override val genericScaleAttribute: Attribute? = Attribute.SCALE

    override val genericJumpStrengthAttribute: Attribute = Attribute.JUMP_STRENGTH

    @Suppress("UnstableApiUsage")
    override fun transferDamageEvent(entity: LivingEntity, event: EntityDamageEvent) {
        entity.damage(event.damage, event.damageSource)
    }

    @EventHandler
    fun onEntityDismount(event: EntityDismountEvent) {
        event.isCancelled = !FantasyEntityDismountEvent(
            event.entity,
            event.dismounted,
            event.isCancellable
        ).callEvent()
    }

    @EventHandler
    fun onEntityMount(event: EntityMountEvent) {
        event.isCancelled = !FantasyEntityMountEvent(event.entity, event.mount).callEvent()
    }

    override fun getFortuneEnchantment(): Enchantment = Enchantment.FORTUNE

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

    override fun duplicateAllay(allay: Allay): Boolean {
        if (allay.duplicationCooldown > 0) return false
        allay.duplicateAllay()
        (allay.world as CraftWorld).handle
            .broadcastEntityEvent((allay as CraftAllay).handle, 18.toByte())
        return true
    }

    override fun boostArrow(arrow: Arrow) {
        for (effect in arrow.basePotionType?.potionEffects.orEmpty()) {
            arrow.addCustomEffect(
                effect.withDuration(effect.duration).withAmplifier(effect.amplifier + 1),
                true
            )
        }
    }

    override fun applyFont(component: Component, font: Key): Component {
        return component.font(font)
    }

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
            Predicate { livingEntity ->
                val player = livingEntity.bukkitEntity as? Player
                if (player != null) {
                    if (hasAbility.test(player)) {
                        return@Predicate (!hasKey.test(creeper))
                    }
                }
                false
            }

        ).asPaperGoal()
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

    override val armorAttribute: Attribute = Attribute.ARMOR
    override val maxHealthAttribute: Attribute = Attribute.MAX_HEALTH
    override val movementSpeedAttribute: Attribute = Attribute.MOVEMENT_SPEED
    override val attackDamageAttribute: Attribute = Attribute.ATTACK_DAMAGE
    override val flyingSpeedAttribute: Attribute = Attribute.FLYING_SPEED
    override val attackKnockbackAttribute: Attribute = Attribute.ATTACK_KNOCKBACK
    override val attackSpeedAttribute: Attribute = Attribute.ATTACK_SPEED
    override val armorToughnessAttribute: Attribute = Attribute.ARMOR_TOUGHNESS
    override val luckAttribute: Attribute = Attribute.LUCK
    override val horseJumpStrengthAttribute: Attribute = Attribute.JUMP_STRENGTH
    override val spawnReinforcementsAttribute: Attribute = Attribute.SPAWN_REINFORCEMENTS
    override val followRangeAttribute: Attribute = Attribute.FOLLOW_RANGE
    override val knockbackResistanceAttribute: Attribute = Attribute.KNOCKBACK_RESISTANCE
    override val fallDamageMultiplierAttribute: Attribute? = Attribute.FALL_DAMAGE_MULTIPLIER
    override val maxAbsorptionAttribute: Attribute? = Attribute.MAX_ABSORPTION
    override val safeFallDistanceAttribute: Attribute? = Attribute.SAFE_FALL_DISTANCE
    override val scaleAttribute: Attribute? = Attribute.SCALE
    override val stepHeightAttribute: Attribute? = Attribute.STEP_HEIGHT
    override val gravityAttribute: Attribute? = Attribute.GRAVITY
    override val jumpStrengthAttribute: Attribute? = Attribute.JUMP_STRENGTH
    override val burningTimeAttribute: Attribute? = Attribute.BURNING_TIME
    override val explosionKnockbackResistanceAttribute: Attribute? = Attribute.EXPLOSION_KNOCKBACK_RESISTANCE
    override val movementEfficiencyAttribute: Attribute? = Attribute.MOVEMENT_EFFICIENCY
    override val oxygenBonusAttribute: Attribute? = Attribute.OXYGEN_BONUS
    override val waterMovementEfficiencyAttribute: Attribute? = Attribute.WATER_MOVEMENT_EFFICIENCY
    override val temptRangeAttribute: Attribute? = Attribute.TEMPT_RANGE
    override val nauseaEffect: PotionEffectType = PotionEffectType.NAUSEA
    override val miningFatigueEffect: PotionEffectType = PotionEffectType.MINING_FATIGUE
    override val hasteEffect: PotionEffectType = PotionEffectType.HASTE
    override val unbreakingEnchantment: Enchantment = Enchantment.UNBREAKING
    override val efficiencyEnchantment: Enchantment = Enchantment.EFFICIENCY
    override val respirationEnchantment: Enchantment = Enchantment.RESPIRATION
    override val jumpBoostEffect: PotionEffectType = PotionEffectType.JUMP_BOOST
    override val aquaAffinityEnchantment: Enchantment = Enchantment.AQUA_AFFINITY
    override val slownessEffect: PotionEffectType = PotionEffectType.SLOWNESS
    override val baneOfArthropodsEnchantment: Enchantment = Enchantment.BANE_OF_ARTHROPODS
    override val strengthEffect: PotionEffectType = PotionEffectType.STRENGTH
    override val blockInteractionRangeAttribute: Attribute? = Attribute.BLOCK_INTERACTION_RANGE
    override val entityInteractionRangeAttribute: Attribute? = Attribute.ENTITY_INTERACTION_RANGE
    override val blockBreakSpeedAttribute: Attribute? = Attribute.BLOCK_BREAK_SPEED
    override val miningEfficiencyAttribute: Attribute? = Attribute.MINING_EFFICIENCY
    override val sneakingSpeedAttribute: Attribute? = Attribute.SNEAKING_SPEED
    override val submergedMiningSpeedAttribute: Attribute? = Attribute.SUBMERGED_MINING_SPEED
    override val sweepingDamageRatioAttribute: Attribute? = Attribute.SWEEPING_DAMAGE_RATIO

    override fun setCustomModelData(meta: ItemMeta, cmd: Int): ItemMeta {
        val component = meta.customModelDataComponent
        component.strings = listOf(cmd.toString())
        meta.setCustomModelDataComponent(component)
        return meta
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
            true,
            0,
            Optionull.map(serverPlayer.chatSession) { obj ->
                obj!!.asData()
            }
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
        try {
            val packInfo = ResourcePackInfo.resourcePackInfo()
                .uri(URI.create(pack))
                .computeHashAndBuild().get()
            val packs: MutableList<ResourcePackInfo?> = ArrayList<ResourcePackInfo?>()
            packs.add(packInfo)
            for (originsReforgedResourcePackInfo in extraPacks.values) {
                val info = originsReforgedResourcePackInfo.packInfo as? ResourcePackInfo
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
        OriginsReforgedBlockDamageAbortEvent(event.player, event.getBlock(), event.itemInHand).callEvent()
    }

    override val ominousBottle: Material? = Material.OMINOUS_BOTTLE
}