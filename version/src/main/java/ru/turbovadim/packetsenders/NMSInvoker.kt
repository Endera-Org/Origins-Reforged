package ru.turbovadim.packetsenders

import com.destroystokyo.paper.entity.ai.Goal
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.TriState
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.potion.PotionEffectType
import java.util.function.Predicate

abstract class NMSInvoker : Listener {

    abstract fun dealThornsDamage(target: Entity, amount: Int, attacker: Entity)

    abstract fun getSmiteEnchantment(): Enchantment

    abstract fun getElderGuardianParticle(): Particle

    abstract fun getWitchParticle(): Particle

    abstract fun damageItem(item: ItemStack, amount: Int, player: Player)

    abstract fun startAutoSpinAttack(player: Player, duration: Int, riptideAttackDamage: Float, item: ItemStack)

    abstract fun tridentMove(player: Player)

    abstract fun getIronGolemAttackGoal(golem: LivingEntity, hasAbility: Predicate<Player>): Goal<Mob>

    abstract fun bounce(player: Player)

    abstract fun getGenericScaleAttribute(): Attribute?

    abstract fun transferDamageEvent(entity: LivingEntity, event: EntityDamageEvent)

    abstract fun getGenericJumpStrengthAttribute(): Attribute

    abstract fun getFortuneEnchantment(): Enchantment

    abstract fun duplicateAllay(allay: Allay): Boolean

    abstract fun launchArrow(projectile: Entity, entity: Entity, roll: Float, force: Float, divergence: Float)

    abstract fun boostArrow(arrow: Arrow)

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

    abstract fun sendResourcePacks(
        player: Player,
        pack: String,
        extraPacks: MutableMap<*, OriginsReforgedResourcePackInfo>
    )

    abstract val nauseaEffect: PotionEffectType

    abstract val miningFatigueEffect: PotionEffectType

    abstract val hasteEffect: PotionEffectType

    abstract val jumpBoostEffect: PotionEffectType

    abstract val slownessEffect: PotionEffectType

    abstract val strengthEffect: PotionEffectType

    abstract val unbreakingEnchantment: Enchantment

    abstract val efficiencyEnchantment: Enchantment

    abstract val respirationEnchantment: Enchantment

    abstract val aquaAffinityEnchantment: Enchantment

    abstract val baneOfArthropodsEnchantment: Enchantment

    abstract fun getRespawnLocation(player: Player): Location?

    abstract fun resetRespawnLocation(player: Player)

    abstract fun getAttributeModifier(instance: AttributeInstance, key: NamespacedKey): AttributeModifier?

    abstract fun dealDryOutDamage(entity: LivingEntity, amount: Int)

    abstract fun dealDrowningDamage(entity: LivingEntity, amount: Int)

    abstract fun dealFreezeDamage(entity: LivingEntity, amount: Int)

    open fun supportsInfiniteDuration(): Boolean {
        return true
    }

    abstract fun isUnderWater(entity: LivingEntity): Boolean

    abstract fun knockback(entity: LivingEntity, strength: Double, x: Double, z: Double)

    abstract fun setFlyingFallDamage(player: Player, state: TriState)

    abstract fun broadcastSlotBreak(player: Player, slot: EquipmentSlot, players: MutableCollection<Player>)

    abstract fun sendBlockDamage(player: Player, location: Location, damage: Float, entity: Entity)

    abstract fun addAttributeModifier(
        instance: AttributeInstance,
        key: NamespacedKey,
        name: String,
        amount: Double,
        operation: AttributeModifier.Operation
    )

    abstract fun setWorldBorderOverlay(player: Player, show: Boolean)

    abstract fun applyFont(component: Component, font: Key): Component

    open val ominousBottle: Material?
        get() = null

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
}