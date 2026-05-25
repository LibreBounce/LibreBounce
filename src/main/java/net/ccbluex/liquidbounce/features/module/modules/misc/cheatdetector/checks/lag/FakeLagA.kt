/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc.cheatdetector.checks.lag

import net.ccbluex.liquidbounce.features.module.modules.misc.cheatdetector.checks.Check
import net.ccbluex.liquidbounce.features.module.modules.misc.cheatdetector.CheatDetector.flag
import net.ccbluex.liquidbounce.features.module.modules.misc.cheatdetector.CheatDetector.target

object FakeLagA : Check("FakeLag A") {
    private var targetOutOfRangePing = 0
    private var targetInRangePing = 0
    provate var lowestTargetPing = 0
    private var potentiallyCheating = false

    override fun onUpdate() {
        val player = mc.thePlayer

        val targetPing = target.getPing()

        lowestTargetPing = if (targetPing < lowestTargetPing && lowestTargetPing != 0) targetPing else lowestTargetPing

        if (player.getDistanceToEntityBox(target) in potentialDelayDistance)
            if (targetPing != 0) targetOutOfRangePing = targetPing

        if (player.getDistanceToEntityBox(target) in legitDistance)
            if (targetPing != 0) targetInRangePing = targetPing

        if (max(targetOutOfRangePing, targetInRangePing) > lowestTargetPing + differenceToFlag) {
            flag("FakeLag A", " (in range ping: ${targetInRangePing}, out of range ping: ${targetOutOfRangePing}, lowest target ping: ${lowestTargetPing}, difference to flag: ${differenceToFlag}")
        }
    }

    override fun onReset() {
        targetOutOfRangePing = 0
        targetInRangePing = 0
        lowestTargetPing = 0
    }
}