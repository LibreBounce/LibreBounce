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
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.raycastEntity
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.simulation.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Vec3
import kotlin.math.max
import kotlin.math.sqrt

object SmartHit : Module("SmartHit", Category.COMBAT) {

    // TODO: Not on 1-tap option, taking into account your weapon + enchantments, the opponent's armor + enchantments, and potion effects
    // Also add an option that makes it click anyway, if the knockback is large enough to combo you
    private val notAboveRange by float("NotAboveRange", 2.7f, 0f..8f, suffix = "blocks")
    private val notAbovePredRange by float("NotAbovePredictedRange", 2.8f, 0f..8f, suffix = "blocks")
    private val notBelowOwnHealth by float("NotBelowOwnHealth", 5f, 0f..20f)
    private val notBelowEnemyHealth by float("NotBelowEnemyHealth", 5f, 0f..20f)
    private val notOnEdge by boolean("NotOnEdge", false)
    private val notOnEdgeLimit by float("NotOnEdgeLimit", 1f, 0f..8f, suffix = "blocks") { notOnEdge }

    // Prediction
    // Change these values to your preference; however, you should fine-tune PredictEnemyPosition for each server,
    // since they all have different knockback strengths
    private val predictClientMovement by int("PredictClientMovement", 2, 0..5, suffix = "ticks")
    private val predictEnemyPosition by float("PredictEnemyPosition", 1.5f, 0f..2f)

    private val debug by boolean("Debug", false).subjective()

    private var lastHitCrit = false

    val onAttack = handler<AttackEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        lastHitCrit = player.fallDistance > 0
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
         * Currently, however, it only checks 2 ticks ahead, for performance reasons.
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

        val distance = player.getDistanceToEntityBox(target)

        // The reason why I also have this is because the combat range of a player is a beam starting from the eyes
        // in lower/higher ground situations, this effectively means 2 players can have lower or higher range
        // TODO: Have another value that checks own pos from (combinedPing / 2).toTicks() ago, and predict the target position
        // then, to be even more accurate
        val targetDist = target.getDistanceToEntityBox(player)

        val prediction = target.currPos.subtract(target.prevPos).times(predictEnemyPosition.toDouble())
        val boundingBox = target.hitBox.offset(prediction)

        var simDist = player.getDistanceToBox(boundingBox)

        val simPlayer = SimulatedPlayer.fromClientPlayer(RotationUtils.modifiedInput)

        val currPos = player.currPos
        val prevPos = player.prevPos

        repeat(predictClientMovement) {
            simPlayer.tick()
        }

        player.setPosAndPrevPos(simPlayer.pos)

        simDist = player.getDistanceToBox(boundingBox)

        player.setPosAndPrevPos(currPos, prevPos)

        val rotationToPlayer = toRotation(player.hitBox.center, true, target!!)
        val rotDiff = rotationDifference(rotationToPlayer, target.rotation)

        // The ground ticks and simPlayer checks are there since you stay on ground for a tick, before being able to jump
        val properGround = player.onGround && player.groundTicks > 1 && simPlayer.onGround

        // If you are "falling" (as in fallDistance > 0; it doesn't reset when you go up, only when on ground), you can land critical hits
        val falling = player.fallDistance > 0 || simPlayer.fallDistance > 0

        // TODO: Check if you hit the player in the last ticks; latency may affect when the hit lands
        if (target.hurtTime == 0) lastHitCrit = false

        /*
         * If a target is running or cannot hit you, it is not beneficial to hit more than required (i.e., when the target is hittable), since the slowdown
         * may make it impossible to properly chase the target, and in the latter case, the opponent will be confused by your movements
         * This is only here because it is very difficult to have proper rotation prediction, and latency makes it so
         * even if a target is not looking at you client-sidedly (past rotation), that target can still hit you
         * As such, it's better to have it like this
         */
        // TODO: Also consider a target that is holding the backwards key for over 6-10 ticks as not likely to hit, and a target not moving, too
        // TODO: Turn this into an integer (0-100), and have a treshold of when it starts being considered likely
        //if (simDist > distance && distance > 2.8 && player.hurtTime == 0 && target.hurtTime == 0) targetHitLikely = false
        val rotHittable = rotDiff < 30f + (12f * combinedPingMult) && !target.hitBox.isVecInside(player.eyes)
        val targetHitLikely = rotHittable && !target.isUsingItem && (targetDist < 3.05f && distance <= 3f)

        val baseHurtTime = 3f / (1f + sqrt(distance) - (rotDiff / 180f))
        val optimalHurtTime = max(baseHurtTime.toInt(), 2)

        val groundHit = properGround && if (targetHitLikely) target.hurtTime !in 2..optimalHurtTime else target.hurtTime == 0
        val fallingHit = falling && if (targetHitLikely) target.hurtTime !in 2..optimalHurtTime else !lastHitCrit
        val airHit = fallingHit || (target.hurtTime in 4..5 && targetHitLikely)

        val hurtTimeNoEscape = (2 * distance * 8).toInt() / 10

        val shouldHit = when {
            // This currently does not fully account for burst clicking, timed hits, zest tapping, etc
            groundHit || airHit -> true

            // TODO: Instead, simulate both players' positions and check if you can hit on the tick after (or 2 ticks after, or both); if not, hit immediately
            (distance > notAboveRange || simDist > notAbovePredRange) && player.hurtTime !in hurtTimeNoEscape..8 && targetHitLikely -> true

            distance <= 3f && targetDist > 3.12f && target.hurtTime < 2 -> true

            // Panic hitting is also not a very good idea either, n'est-ce pas?
            player.health < notBelowOwnHealth -> true

            // TODO: Instead, calculate whether you can 1-tap your opponent
            // ItemStack.attackDamage defenseFactor
            target.health < notBelowEnemyHealth -> true

            // If you are near an edge, you should hit as much as possible to reduce received knockback
            // I assume this checks for all edges, including ones that are irrelevant
            // TODO: Check if the target is near an edge; if so, you can also spam hit to deal as much knockback as possible
            notOnEdge && player.isNearEdge(notOnEdgeLimit) -> true

            else -> false
        }

        if (debug) chat("(SmartHit) Will hit: ${shouldHit}, predicted distance: ${simDist}, current distance: ${distance}, current distance (target POV): ${targetDist}, combined ping: ${combinedPing}, combined ping multiplier: ${combinedPingMult}, rotation difference: ${rotDiff}, target hit likely: ${targetHitLikely}, own hurttime: ${player.hurtTime}, target hurttime: ${target.hurtTime}, on ground: ${player.onGround}, falling: ${falling}")

        return shouldHit
    }
}