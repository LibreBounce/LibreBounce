/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc.cheatdetector

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.*
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

    val onAttack = handler<AttackEvent> { event ->
        target = event.targetEntity ?: return@handler
    }

    val onUpdate = handler<UpdateEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        target: Entity? = KillAura.target as EntityPlayer? ?: target as EntityPlayer?
        var lastTarget: Entity? = null

        if (target == null) {
            reset()
            return@handler
        }

        if (lastTarget != fixedTarget) {
            reset()
        }

        if (vl >= maxVL)
            chat("(CheatDetector) $fixedTarget is cheating")

        if (vlDecay.resetIfPassed()) {
            vl--
            if (debug) chat("(CheatDetector) Reduced VL by 1")
        }

        lastTarget = fixedTarget

        check.onUpdate()
    }

    private fun reset() {
        vl = 0

        if (debug) chat("(CheatDetector) Reset the target!")

        check.onReset()
    }

    fun flag(checkName: String, debugInformation: String) {
        if (flagTimer.resetIfPassed()) {
            vl++

            val extraInfo = if (debug) debugInformation else ""
            chat("(CheatDetector) %target failed %checkName (${vl}, ${maxVL})" + extraInfo)
        }
    }
}