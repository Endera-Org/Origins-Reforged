package ru.turbovadim.packetsenders

import com.destroystokyo.paper.entity.ai.Goal
import net.kyori.adventure.key.Key
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.TriState
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.attribute.AttributeModifier
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDamageAbortEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.entity.EntityMountEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.potion.PotionEffectType
import java.net.URI
import java.util.concurrent.ExecutionException
import java.util.function.Predicate

@Suppress("UnstableApiUsage")
abstract class NMSInvoker : Listener {

    abstract fun dealExplosionDamage(player: Player, amount: Int)

    abstract fun dealSonicBoomDamage(entity: LivingEntity, amount: Int, source: Player)

    abstract fun getVillagerAfraidGoal(villager: LivingEntity, hasAbility: Predicate<Player>): Goal<Villager>

    abstract fun getNearestVisiblePlayer(piglin: Piglin): Player

    abstract fun throwItem(piglin: Piglin, itemStack: ItemStack, pos: Location)

    abstract fun dealThornsDamage(target: Entity, amount: Int, attacker: Entity)

    open fun getSmiteEnchantment(): Enchantment = Enchantment.SMITE

    open fun getElderGuardianParticle(): Particle = Particle.ELDER_GUARDIAN

    open fun getWitchParticle(): Particle = Particle.WITCH

    open fun damageItem(item: ItemStack, amount: Int, player: Player) {
        item.damage(amount, player)
    }

    abstract fun startAutoSpinAttack(player: Player, duration: Int, riptideAttackDamage: Float, item: ItemStack)

    abstract fun tridentMove(player: Player)

    abstract fun getIronGolemAttackGoal(golem: LivingEntity, hasAbility: Predicate<Player>): Goal<Mob>

    abstract fun bounce(player: Player)

    abstract val genericScaleAttribute: Attribute?

    open fun transferDamageEvent(entity: LivingEntity, event: EntityDamageEvent) {
        entity.damage(event.damage, event.damageSource)
    }

    abstract val genericJumpStrengthAttribute: Attribute

    open fun getFortuneEnchantment(): Enchantment = Enchantment.FORTUNE

    abstract fun duplicateAllay(allay: Allay): Boolean

    abstract fun launchArrow(projectile: Entity, entity: Entity, roll: Float, force: Float, divergence: Float)

    open fun boostArrow(arrow: Arrow) {
        for (effect in arrow.basePotionType?.potionEffects.orEmpty()) {
            arrow.addCustomEffect(
                effect.withDuration(effect.duration).withAmplifier(effect.amplifier + 1),
                true
            )
        }
    }

    abstract fun sendEntityData(player: Player, entity: Entity, bytes: Byte)

    abstract fun getCreeperAfraidGoal(
        creeper: LivingEntity,
        hasAbility: Predicate<Player>,
        hasKey: Predicate<LivingEntity>
    ): Goal<Creeper>

    abstract fun wasTouchingWater(player: Player): Boolean

    abstract fun getDestroySpeed(item: ItemStack, block: Material): Float

    abstract fun getDestroySpeed(block: Material): Float

    abstract fun setNoPhysics(player: Player, noPhysics: Boolean)

    abstract fun sendPhasingGamemodeUpdate(player: Player, gameMode: GameMode)

    open fun sendResourcePacks(
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

    open val nauseaEffect: PotionEffectType = PotionEffectType.NAUSEA

    open val miningFatigueEffect: PotionEffectType = PotionEffectType.MINING_FATIGUE

    open val hasteEffect: PotionEffectType = PotionEffectType.HASTE

    open val jumpBoostEffect: PotionEffectType = PotionEffectType.JUMP_BOOST

    open val slownessEffect: PotionEffectType = PotionEffectType.SLOWNESS

    open val strengthEffect: PotionEffectType = PotionEffectType.STRENGTH

    open val unbreakingEnchantment: Enchantment = Enchantment.UNBREAKING

    open val efficiencyEnchantment: Enchantment = Enchantment.EFFICIENCY

    open val respirationEnchantment: Enchantment = Enchantment.RESPIRATION

    open val aquaAffinityEnchantment: Enchantment = Enchantment.AQUA_AFFINITY

    open val baneOfArthropodsEnchantment: Enchantment = Enchantment.BANE_OF_ARTHROPODS

    open fun getRespawnLocation(player: Player): Location? = player.respawnLocation

    open fun resetRespawnLocation(player: Player) {
        player.respawnLocation = null
    }

    open fun getAttributeModifier(instance: AttributeInstance, key: NamespacedKey): AttributeModifier? =
        instance.getModifier(key)

    open fun dealDryOutDamage(entity: LivingEntity, amount: Int) {
        entity.damage(amount.toDouble(), DamageSource.builder(DamageType.DRY_OUT).build())
    }

    open fun dealDrowningDamage(entity: LivingEntity, amount: Int) {
        entity.damage(amount.toDouble(), DamageSource.builder(DamageType.DROWN).build())
    }

    open fun dealFreezeDamage(entity: LivingEntity, amount: Int) {
        entity.damage(amount.toDouble(), DamageSource.builder(DamageType.FREEZE).build())
    }

    open fun supportsInfiniteDuration(): Boolean {
        return true
    }

    open fun isUnderWater(entity: LivingEntity): Boolean = entity.isUnderWater

    open fun knockback(entity: LivingEntity, strength: Double, x: Double, z: Double) {
        entity.knockback(strength, x, z)
    }

    open fun setFlyingFallDamage(player: Player, state: TriState) {
        player.setFlyingFallDamage(state)
    }

    open fun broadcastSlotBreak(player: Player, slot: EquipmentSlot, players: MutableCollection<Player>) {
        player.broadcastSlotBreak(slot, players)
    }

    open fun sendBlockDamage(player: Player, location: Location, damage: Float, entity: Entity) {
        player.sendBlockDamage(location, damage, entity)
    }

    open fun addAttributeModifier(
        instance: AttributeInstance,
        key: NamespacedKey,
        name: String,
        amount: Double,
        operation: AttributeModifier.Operation
    ) {
        instance.addModifier(AttributeModifier(key, amount, operation, EquipmentSlotGroup.ANY))
    }

    open fun setWorldBorderOverlay(player: Player, show: Boolean) {
        if (show) {
            val border = Bukkit.createWorldBorder()
            border.center = player.world.worldBorder.center
            border.size = player.world.worldBorder.size
            border.warningDistance = (player.world.worldBorder.size * 2).toInt()
            player.worldBorder = border
        } else player.worldBorder = null
    }

    open fun applyFont(component: Component, font: Key): Component = component.font(font)

    open val ominousBottle: Material? = Material.OMINOUS_BOTTLE

    abstract val armorAttribute: Attribute

    abstract val maxHealthAttribute: Attribute

    abstract val movementSpeedAttribute: Attribute

    abstract val flyingSpeedAttribute: Attribute

    abstract val attackDamageAttribute: Attribute

    abstract val attackKnockbackAttribute: Attribute

    abstract val attackSpeedAttribute: Attribute

    abstract val armorToughnessAttribute: Attribute

    abstract val luckAttribute: Attribute

    abstract val horseJumpStrengthAttribute: Attribute

    abstract val spawnReinforcementsAttribute: Attribute

    abstract val followRangeAttribute: Attribute

    abstract val knockbackResistanceAttribute: Attribute

    abstract val fallDamageMultiplierAttribute: Attribute?

    abstract val maxAbsorptionAttribute: Attribute?

    abstract val safeFallDistanceAttribute: Attribute?

    abstract val scaleAttribute: Attribute?

    abstract val stepHeightAttribute: Attribute?

    abstract val gravityAttribute: Attribute?

    abstract val jumpStrengthAttribute: Attribute?

    abstract val burningTimeAttribute: Attribute?

    abstract val explosionKnockbackResistanceAttribute: Attribute?

    abstract val movementEfficiencyAttribute: Attribute?

    abstract val oxygenBonusAttribute: Attribute?

    abstract val waterMovementEfficiencyAttribute: Attribute?

    abstract val temptRangeAttribute: Attribute?

    abstract val blockInteractionRangeAttribute: Attribute?

    abstract val entityInteractionRangeAttribute: Attribute?

    abstract val blockBreakSpeedAttribute: Attribute?

    abstract val miningEfficiencyAttribute: Attribute?

    abstract val sneakingSpeedAttribute: Attribute?

    abstract val submergedMiningSpeedAttribute: Attribute?

    abstract val sweepingDamageRatioAttribute: Attribute?

    abstract fun setCustomModelData(meta: ItemMeta, cmd: Int): ItemMeta

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

    @EventHandler
    fun onBlockDamageAbort(event: BlockDamageAbortEvent) {
        OriginsReforgedBlockDamageAbortEvent(event.player, event.block, event.itemInHand).callEvent()
    }
}
