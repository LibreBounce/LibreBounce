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
import net.ccbluex.liquidbounce.features.module.modules.misc.cheatdetector.checks.lag.FakeLagA
import net.ccbluex.liquidbounce.features.module.modules.misc.cheatdetector.checks.reach.ReachA
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.timing.TickDelayTimer
import net.minecraft.entity.Entity
import net.minecraft.entity.living.LivingEntity
import net.minecraft.entity.living.player.PlayerEntity
import kotlin.math.max

object CheatDetector : Module("CheatDetector", Category.MISC) {

    private val cheatDetectorChecks = arrayOf(
        FakeLagA,
        ReachA
    )

    private val checks = cheatDetectorChecks.map { it.checkName }.toTypedArray()

    val cheatChecks by multiChoices("Checks", checks, setOf("FakeLagA"))
    val potentialDelayDistance by floatRange("PotentialDelayDistance", 5f..8f, 0f..16f) { cheatChecks.contains("FakeLagA") }
    val legitDistance by floatRange("LegitDistance", 3.0f..3.5f, 0f..6f) { cheatChecks.contains("FakeLagA") }
    val differenceToFlag by int("DifferenceToFlag", 30, 0..1000, suffix = "ms") { cheatChecks.contains("FakeLagA") }

    private val flagDelay by int("FlagDelay", 10, 0..40, suffix = "ticks")

    private val maxVL by int("MaxVL", 60, 0..200)
    private val vlDecayTime by int("VLDecayTime", 2, 0..20, suffix = "seconds")

    private val verbose by boolean("Verbose", false)
    private val debug by boolean("Debug", false)


    var target: Entity? = null

    var vl = 0

    val vlDecay = TickDelayTimer(vlDecayTime * 20)
    val flagTimer = TickDelayTimer(flagDelay)

    val onAttack = handler<AttackEvent> { event ->
        target = event.targetEntity ?: return@handler
    }

    val onUpdate = handler<UpdateEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        target = KillAura.target as Entity ?: target

        var lastTarget: Entity? = null

        if (target == null) {
            reset()
            return@handler
        }

        if (lastTarget != target) {
            reset()
        }

        if (vlDecay.resetIfPassed()) {
            vl--
            if (debug) chat("(CheatDetector) Reduced VL by 1")
        }

        if (vl >= maxVL)
            chat("(CheatDetector) $target is cheating")

        lastTarget = target

        check.forEach { it.onUpdate() }
    }

    private fun reset() {
        vl = 0

        if (debug) chat("(CheatDetector) Reset the VL to 0, as the target has changed")

        check.forEach { it.onReset() }
    }

    fun flag(checkName: String, debugInformation: String) {
        val extraInfo = if (debug) debugInformation else ""

        if (flagTimer.resetIfPassed()) {
            vl++

            chat("(CheatDetector) $target failed $checkName (${vl}, ${maxVL})" + extraInfo)
        } else {
            if (verbose) chat("(CheatDetector) VERBOSE: $target failed $checkName (${vl}, ${maxVL})" + extraInfo)
        }
    }

    private val check
        get() = cheatDetectorChecks.filter { cheatChecks.contains(it.checkName) }!!
}