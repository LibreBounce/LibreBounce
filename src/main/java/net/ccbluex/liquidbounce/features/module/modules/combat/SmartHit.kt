/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
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
import net.minecraft.util.MathHelper
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.math.PI

object SmartHit : Module("SmartHit", Category.COMBAT) {

    // TODO: Not on 1-tap option, taking into account your weapon + enchantments, the opponent's armor + enchantments, and potion effects
    // Also add an option that makes it click anyway, if the knockback is large enough to combo you
    private val attackableHurtTime by intRange("AttackableHurtTime", 0..0, 0..10)
    private val notAboveRange by float("NotAboveRange", 2.7f, 0f..8f, suffix = "blocks")
    private val notAbovePredRange by float("NotAbovePredictedRange", 2.8f, 0f..8f, suffix = "blocks")
    private val notBelowOwnHealth by float("NotBelowOwnHealth", 5f, 0f..20f)
    private val notBelowEnemyHealth by float("NotBelowEnemyHealth", 5f, 0f..20f)
    private val notOnEdge by boolean("NotOnEdge", false)
    private val notOnEdgeLimit by float("NotOnEdgeLimit", 1f, 0f..8f, suffix = "blocks") { notOnEdge }

    // Change these values to your preference; you should fine-tune PredictEnemyPosition for each server,
    // since they are all slightly different
    private val predictClientMovement by int("PredictClientMovement", 5, 0..5, suffix = "ticks")
    private val predictEnemyPosition by float("PredictEnemyPosition", 1.5f, 0f..2f)

    // Allows you to alter how strong the simulated knockback is
    private val simulateKnockback by boolean("SimulateKnockback", true)
    private val simulatedHorizontalKnockback by floatRange("SimulatedHorizontalKnockback", 0.88f..1f, 0f..4f)
    private val simulatedVerticalKnockback by floatRange("SimulatedVerticalKnockback", 0.4f..0.5f, 0f..2f)

    private val debug by boolean("Debug", false).subjective()

    private var simHurtTime = 0
    private var lastHitCrit = false
    private var hitOnTheWay = false

    val onAttack = handler<AttackEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        val target = event.targetEntity ?: return@handler

        lastHitCrit = player.fallDistance > 0

        hitOnTheWay = player.getDistanceToEntityBox(target) < 3f && (target as EntityLivingBase).hurtTime in attackableHurtTime
    }

    fun shouldHit(target: Entity): Boolean {
        /*
         * This is the code responsible for SmartHit.
         *
         * This would optimally have calculations for every tick, and simulate when you can or cannot hit.
         * It can get more complicated than that, though, since both the player and the target can do plenty of things
         * that affect calculations, rendering them inaccurate - as such, this would take more than the current code.
         * If you can hit now but not in the next tick (or in the one after), that means you should hit now, and continue reducing
         * to not get comboed.
         *
         * No edge case has been implemented that does (at the very least) rudimentary future knockback calculations, which should be
         * as customizable as possible, to be accurate on all servers (given the right config, of course).
         *
         * Currently, however, it only checks the ticks ahead as defined by PredictClientMovement, for performance reasons.
         *
         * Another thing that must be implemented is client-side target hurttime checking.
         * Say you have 200 ping; in about half that time, the packet will be received by the server; in the other half,
         * you will see the attack itself. As such, you are seeing what has happened 2-4 ticks ago.
         *
         * Credits to all the Raven versions, Augustus, and Vape for some of these ideas!
         */
        val player = mc.thePlayer ?: return false

        // Latency affects many things, so it is worth to be included in our calculations
        val playerPing = (player as EntityPlayer).getPing()
        val targetPing = (target as EntityPlayer).getPing()
        val combinedPing = playerPing + targetPing
        val combinedPingMult = combinedPing.toFloat() / 100f

        /*
         * The combat range of a player is a beam starting from the eyes.
         * This gives a low ground advantage, which can only be calculated in this manner.
         * As such, calculating both distances is paramount for good hit selecting.
         *
         * One thing is missing, for now; that is, also predicting the true POV of the target.
         */
        val distance = player.getDistanceToEntityBox(target)
        val targetDistance = target.getDistanceToEntityBox(player)

        val simPlayer = SimulatedPlayer.fromClientPlayer(RotationUtils.modifiedInput)
        simHurtTime = player.hurtTime

        repeat(predictClientMovement + 1) {
            simPlayer.tick()

            if (simHurtTime > 0) --simHurtTime
        }

        // The ground ticks and simPlayer checks are there since you stay on ground for a tick, before being able to jump
        // This does not account for getting hit, either
        val trueGround = player.onGround && player.groundTicks > 1 && simPlayer.onGround

        // If you are "falling" (as in fallDistance > 0; it doesn't reset when you go up, only when on ground), you can land critical hits
        val falling = player.fallDistance > 0 || simPlayer.fallDistance > 0

        val targetHittable = target.hurtTime in attackableHurtTime

        if (target.hurtTime <= attackableHurtTime.last) lastHitCrit = false

        // TODO: Fix this requiring you to give the first hit
        // Perhaps a better implementation could be made, which would do hurtTime + (playerPing / 2), when you hit the opponent
        if (!targetHittable) hitOnTheWay = false

        /*
         * If a target is running or cannot hit you, it is not beneficial to hit more than required (i.e. when the target can be hit), since the slowdown
         * will make it much harder to properly chase the target, and in the latter case, the opponent will be confused by your movements.
         *
         * A great idea would be to have this as an integer (chance that the target will hit you), and let the user
         * decide when it should spam hit or time hits. Additionally, it should predict if the target is holding S, and also time hits if so.
         */
        val rotDiff = rotationDifference(
            toRotation(player.hitBox.center, true, target!!),
            target.rotation
        )

        /*
         * This is only here because it is very difficult to have proper rotation prediction, and latency makes it so,
         * even if a target is not looking at your latest pos client-sidedly (because that is a past rotation), that target can still hit you.
         * As such, it's better to have it like this.
         *
         * Another alternative is to keep a list of previous positions, and figure out if the target is aiming at one of them,
         * presumably the one from the visual delay ago (let's initially assume combined ping, and then adjust).
         */
        val rotHittable = rotDiff < 22f + (10f * combinedPingMult) && !target.hitBox.isVecInside(player.eyes)
        val targetHitLikely = rotHittable && !target.isUsingItem && targetDistance < 3.08f

        val simulatedDistance = simulateDistance(simPlayer, target, simulateKnockback && targetHitLikely)

        // Many magic numbers, but this is the best implementation I've done, so far
        val baseHurtTime = 3f / (1f + sqrt(distance) - (rotDiff / 180f))
        val optimalHurtTime = max(baseHurtTime.toInt(), attackableHurtTime.last + 1)
        val hurtTimeNoEscape = (2 * distance * 8).toInt() / 10

        val groundHit = trueGround && if (targetHitLikely) target.hurtTime !in 2..optimalHurtTime else targetHittable && !hitOnTheWay

        val fallingHit = falling && if (targetHitLikely) target.hurtTime !in (attackableHurtTime.last + 1)..optimalHurtTime else targetHittable && (!hitOnTheWay || !lastHitCrit)
        val airHit = fallingHit || (target.hurtTime in 4..5 && targetHitLikely)

        val shouldHit = when {
            // This currently does not fully account for burst clicking, timed hits, zest tapping, etc, but it is a start
            groundHit || airHit -> true

            (distance > notAboveRange || simulatedDistance > notAbovePredRange) && player.hurtTime !in hurtTimeNoEscape..8 && targetHitLikely -> true

            // Hits the opponent when the opponent can't hit you; should give plenty of free hits
            targetDistance > 3.05f && targetHittable -> true

            // Panic hitting is also not a very good idea either, n'est-ce pas?
            player.health < notBelowOwnHealth -> true

            // TODO: Instead, calculate whether you can 1-tap your opponent
            // ItemStack.attackDamage defenseFactor
            target.health < notBelowEnemyHealth -> true

            // If you are near an edge, you should hit as much as possible to reduce received knockback
            // TODO: Check if the target is near an edge; if so, you can also spam hit to deal as much knockback as possible
            notOnEdge && player.isNearEdge(notOnEdgeLimit) -> true

            else -> false
        }

        if (debug) chat("(SmartHit) Will hit: ${shouldHit}, hit on the way: ${hitOnTheWay}, current distance: ${distance}, current distance (target POV): ${targetDistance}, predicted distance: ${simulatedDistance}, combined ping: ${combinedPing}, combined ping multiplier: ${combinedPingMult}, rotation difference: ${rotDiff}, target hit likely: ${targetHitLikely}, own hurttime: ${player.hurtTime}, simulated own hurttime: ${simHurtTime}, target hurttime: ${target.hurtTime}, on ground: ${player.onGround}, predicted ground: ${simPlayer.onGround}, falling: ${falling}")

        return shouldHit
    }

    // TODO: Maybe turn this into a util, as it is commonly used
    private fun simulateDistance(simPlayer: SimulatedPlayer, target: Entity, simulateKnockback: Boolean): Double {
        val player = mc.thePlayer ?: return 0.0

        val prediction = target.currPos.subtract(target.prevPos).times(predictEnemyPosition.toDouble())
        val targetBox = target.hitBox.offset(prediction)

        if (simulateKnockback && simHurtTime <= 0) simulateOwnKnockback(simPlayer, target)

        val (currPos, prevPos) = player.currPos to player.prevPos

        player.setPosAndPrevPos(simPlayer.pos)

        val distance = player.getDistanceToBox(targetBox)

        player.setPosAndPrevPos(currPos, prevPos)

        return distance
    }

    private fun simulateOwnKnockback(simPlayer: SimulatedPlayer, target: Entity) {
        /*
         * This is an extremely hacky way of simulating the knockback you will take,
         * and does not account for knockback enchantments, future positions, or anything else of the sort.
         * Indeed, I would recommend for a recode of this, to ensure maintainabiity.
         */
        //val knockbackModifier = getKnockbackModifier(target as EntityLivingBase)
        val knockbackModifier = simulatedHorizontalKnockback.random()

        // This is where the knockback calculating magic happens
        val knockbackX = -MathHelper.sin(target.rotationYaw * (PI.toFloat() / 180.0f)) * knockbackModifier * 0.5f
        val knockbackY = simulatedVerticalKnockback.random()
        val knockbackZ = MathHelper.cos(target.rotationYaw * (PI.toFloat() / 180.0f)) * knockbackModifier * 0.5f

        // Apply knockback
        simPlayer.motionX += knockbackX
        simPlayer.motionY += knockbackY
        simPlayer.motionZ += knockbackZ

        if (debug) chat("(SmartHit) Simulated knockback. X: ${knockbackX}, Y: ${knockbackY}, Z: ${knockbackZ}, modifier: ${knockbackModifier}")

        // It is expected that, if this reduces, it will be like the normal damage cycle
        // However, SmartHit going out of range, this, that, may break this
        simHurtTime = 10
    }
}