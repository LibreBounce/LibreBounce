/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.simulation.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.timing.TickDelayTimer
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import kotlin.math.max

object CheatDetector : Module("CheatDetector", Category.MISC) {

    private val flagDelay by int("FlagDelay", 10, 0..40, suffix = "ticks")

    private val maxVL by int("MaxVL", 60, 0..200)
    private val vlDecayTime by int("VLDecayTime", 2, 0..20, suffix = "seconds")

    private val debug by boolean("Debug", false)
    private val lagBased by boolean("LagBased", true)
    private val potentialDelayDistance by floatRange("PotentialDelayDistance", 5f..8f, 0f..16f) { lagBased }
    private val legitDistance by floatRange("LegitDistance", 3.0f..3.5f, 0f..6f) { lagBased }

    private val differenceToFlag by int("DifferenceToFlag", 30, 0..1000, suffix = "ms") { lagBased }

    var target: Entity? = null

    var vl = 0

    val vlDecay = TickDelayTimer(vlDecayTime * 20)
    val flagTimer = TickDelayTimer(flagDelay)

    var targetOutOfRangePing = 0
    var targetInRangePing = 0
    var lowestTargetPing = 0
    var potentiallyCheating = false

    val onAttack = handler<AttackEvent> { event ->
        target = event.targetEntity ?: return@handler
    }

    val onUpdate = handler<UpdateEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        val fixedTarget: Entity? = KillAura.target ?: target
        val lastTarget: Entity? = null

        if (fixedTarget == null) {
            reset()
            return@handler
        }

        if (lastTarget != fixedTarget) {
            reset()
        }

        if (checkFakeLagging) checkFakeLagging(fixedTarget)

        if (vl >= maxVL)
            chat("(CheatDetector) $fixedTarget is cheating")
        if (vlDecay.resetIfPassed())
            vl--

        lastTarget = fixedTarget
    }

    private fun reset() {
        vl = 0

        targetOutOfRangePing = 0
        targetInRangePing = 0
        lowestTargetPing = 0
    }

    private fun checkFakeLagging(target: Entity) {
        val player = mc.thePlayer

        val targetPing = (target as EntityPlayer).getPing()

        lowestTargetPing = if (targetPing < lowestTargetPing && lowestTargetPing != 0) targetPing else lowestTargetPing

        if (player.getDistanceToEntityBox(target) in potentialDelayDistance)
            targetOutOfRangePing = targetPing

        if (player.getDistanceToEntityBox(target) in legitDistance)
            targetInRangePing = targetPing

        if (max(targetOutOfRangePing, targetInRangePing) > lowestTargetPing + differenceToFlag &&
            flagTimer.resetIfPassed
        ) {
            chat("(CheatDetector) Lag-based module(s) (${vl}, ${maxVL}) (real ping: ${targetInRangePing}, potential delay: ${targetOutOfRangePing}, difference to flag: ${differenceToFlag})")
            vl++
        }
    }
}