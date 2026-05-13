/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.simulation.SimulatedPlayer
import net.minecraft.enchantment.EnchantmentHelper.getKnockbackModifier
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.potion.Potion.blindness
import net.minecraft.util.MathHelper
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.math.PI

object SmartHit : Module("SmartHit", Category.COMBAT) {

    private val usePredictedTargetHurtTime by boolean("UsePredictedTargetHurtTime", true)
    private val attackDelay by int("AttackDelay", 10, 0..10)
    private val experimentalChecks by boolean("ExperimentalChecks", true)
    private val notAboveRange by float("NotAboveRange", 2.7f, 0f..8f, suffix = "blocks")
    private val notAbovePredRange by float("NotAbovePredictedRange", 2.8f, 0f..8f, suffix = "blocks")
    private val checkForCriticalHits by boolean("CheckForCriticalHits", true)
    private val checkForBlockedHits by boolean("CheckForBlockedHits", true)
    private val notBelowOwnHealth by float("NotBelowOwnHealth", 5f, 0f..20f)
    private val notBelowEnemyHealth by float("NotBelowEnemyHealth", 5f, 0f..20f)
    private val notOnEdge by boolean("NotOnEdge", false)
    private val notOnEdgeLimit by float("NotOnEdgeLimit", 1f, 0f..8f, suffix = "blocks") { notOnEdge }

    private val predictClientMovement by int("PredictClientMovement", 5, 0..5, suffix = "ticks")
    private val predictEnemyPosition by float("PredictEnemyPosition", 1.5f, 0f..2f)

    private val simulateKnockback by boolean("SimulateKnockback", true)
    private val simulatedHorizontalKnockback by floatRange("SimulatedHorizontalKnockback", 0.88f..1f, 0f..4f)
    private val simulatedVerticalKnockback by floatRange("SimulatedVerticalKnockback", 0.4f..0.5f, 0f..2f)

    private val debug by boolean("Debug", false).subjective()

    private var simHurtTime = 0
    private var simTargetHurtTime = 0
    private var ticksSinceHit = 0
    private var hitOnTheWay = false
    private var lastHitCrit = false
    private var lastHitBlocked = false

    val onAttack = handler<AttackEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        val target = event.targetEntity ?: return@handler

        val targetPlayer = target as EntityPlayer

        val playerPing = (player as EntityPlayer).getPing()
        val playerLatencyInTicks = playerPing.ceilDiv(2).ceilDiv(20)
        val targetHurtTime = targetPlayer.hurtTime

        simTargetHurtTime = targetHurtTime - playerLatencyInTicks

        simTargetHurtTime = if (usePredictedTargetHurtTime)
            if (simTargetHurtTime <= 10 - attackDelay)
            10 + playerLatencyInTicks else simTargetHurtTime
            else targetHurtTime

        if (simTargetHurtTime <= 10 - attackDelay)
            hitOnTheWay = true

        lastHitCrit = canCritHit(player)
        lastHitBlocked = targetPlayer.isBlocking

        if (ticksSinceHit > playerLatencyInTicks && targetHurtTime == 0)
            hitOnTheWay = false
    }

    val onGameTick = handler<GameTickEvent> { event ->
        simTargetHurtTime--
        ticksSinceHit++
    }

    fun shouldHit(target: Entity): Boolean {
        val player = mc.thePlayer ?: return false

        val playerPing = (player as EntityPlayer).getPing()
        val targetPing = (target as EntityPlayer).getPing()
        val combinedPing = playerPing + targetPing
        val combinedPingMult = combinedPing.toFloat() / 100f

        val distance = player.getDistanceToEntityBox(target)
        val targetDistance = target.getDistanceToEntityBox(player)

        val simPlayer = SimulatedPlayer.fromClientPlayer(RotationUtils.modifiedInput)
        simHurtTime = player.hurtTime

        repeat(predictClientMovement + 1) {
            simPlayer.tick()

            if (simHurtTime > 0) --simHurtTime
        }

        val targetHittable = simTargetHurtTime <= 10 - attackDelay

        if (targetHittable) {
            lastHitCrit = false
            hitOnTheWay = false
        }

        val rotDiff = rotationDifference(
            toRotation(player.hitBox.center, true, target!!),
            target.rotation
        )

        val targetCanHit = rotDiff < 22f + (10f * combinedPingMult) && !target.hitBox.isVecInside(player.eyes)
        val targetHitLikely = targetCanHit && !target.isUsingItem && targetDistance < 3.08f

        val simDistance = simulateDistance(simPlayer, target, simulateKnockback && targetHitLikely)

        val groundHit =
            player.onGround && player.groundTicks > 1 && simPlayer.onGround &&
            targetHittable && !hitOnTheWay
    
        val airHit =
            (targetHittable && !hitOnTheWay) ||
            (checkForCriticalHits && canCritHit(player) && !lastHitCrit)

        val baseHurtTime = 3f / (1f + sqrt(distance) - (rotDiff / 180f))
        val hurtTimeNoEscape = (2 * distance * 8).toInt() / 10
            
        val shouldHit = when {
            groundHit || airHit -> true
            checkForBlockedHits && lastHitBlocked && !target.isBlocking -> true
            experimentalChecks && (distance > notAboveRange || simDistance > notAbovePredRange) && player.hurtTime !in hurtTimeNoEscape..8 && targetHitLikely -> true
            experimentalChecks && targetDistance > 3.05f && targetHittable -> true
            player.health < notBelowOwnHealth -> true
            target.health < notBelowEnemyHealth -> true
            notOnEdge && player.isNearEdge(notOnEdgeLimit) -> true
            target.isDead -> false
            else -> false
        }

        if (debug) chat("(SmartHit) Will hit: ${shouldHit}, hit on the way: ${hitOnTheWay}, last hit blocked: ${lastHitBlocked}, current distance: ${distance}, current distance (target POV): ${targetDistance}, predicted distance: ${simDistance}, combined ping: ${combinedPing}, combined ping multiplier: ${combinedPingMult}, rotation difference: ${rotDiff}, target hit likely: ${targetHitLikely}, own hurttime: ${player.hurtTime}, simulated own hurttime: ${simHurtTime}, target hurttime: ${target.hurtTime}, simulated target hurt time: ${simTargetHurtTime}, on ground: ${player.onGround}, predicted ground: ${simPlayer.onGround}, can critical hit: ${canCritHit(player)}")

        return shouldHit
    }

    private fun simulateDistance(simPlayer: SimulatedPlayer, target: Entity, simulateKnockback: Boolean): Double {
        val player = mc.thePlayer ?: return 0.0

        val targetBox = target.hitBox.offset(
            target.currPos.subtract(target.prevPos).times(predictEnemyPosition.toDouble())
        )

        if (simulateKnockback && simHurtTime <= 10 - attackDelay)
            simulateOwnKnockback(simPlayer, target)

        val (currPos, prevPos) = player.currPos to player.prevPos
        player.setPosAndPrevPos(simPlayer.pos)
        val distance = player.getDistanceToBox(targetBox)
        player.setPosAndPrevPos(currPos, prevPos)
        return distance
    }

    private fun canCritHit(player: EntityPlayer): Boolean =
        player.fallDistance > 0 &&
        !player.isOnLadder &&
        !player.isInWater &&
        !player.isPotionActive(blindness) &&
        player.ridingEntity == null

    private fun simulateOwnKnockback(simPlayer: SimulatedPlayer, target: Entity) {
        val modifier = simulatedHorizontalKnockback.random()
        val fullModifier = (target.rotationYaw * (PI.toFloat() / 180.0f)) * modifier * 0.5f

        val knockbackX = -MathHelper.sin(fullModifier)
        val knockbackY = simulatedVerticalKnockback.random()
        val knockbackZ = MathHelper.cos(fullModifier)

        simPlayer.apply {
            motionX += knockbackX
            motionY += knockbackY
            motionZ += knockbackZ
        }

        if (debug) chat("(SmartHit) Simulated knockback. X: ${knockbackX}, Y + vertical modifier: ${knockbackY}, Z: ${knockbackZ}, horizontal modifier: ${modifier}")

        simHurtTime = attackDelay
    }
}
