/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spartan

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextDouble

object SpartanYPort : SpeedMode("SpartanYPort") {
    private var airMoves = 0

    override fun onMotion() {
        mc.thePlayer?.run {
            if (mc.gameSettings.keyBindForward.isKeyDown) {
                if (onGround) {
                    tryJump()
                    airMoves = 0
                } else {
                    mc.timer.timerSpeed = 1.08f
                    if (airMoves >= 3) jumpMovementFactor = 0.0275f
                    if (airMoves >= 4 && airMoves % 2 == 0) {
                        motionY = -0.32 - nextDouble(endInclusive = 0.009)
                        jumpMovementFactor = 0.0238f
                    }
                    airMoves++
                }
            }
        }
    }
}