/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.timing.TickTimer

object Frame : SpeedMode("Frame") {
    private var motionTicks = 0
    private var move = false
    private val tickTimer = TickTimer()

    override fun onMotion() {
        mc.thePlayer?.run {
            if (isMoving) {
                val speed = 4.25
                if (onGround) {
                    tryJump()
                    if (motionTicks == 1) {
                        tickTimer.reset()
                        if (move) {
                            motionX = 0.0
                            motionZ = 0.0
                            move = false
                        }
                        motionTicks = 0
                    } else motionTicks = 1
                } else if (!move && motionTicks == 1 && tickTimer.hasTimePassed(5)) {
                    motionX *= speed
                    motionZ *= speed
                    move = true
                }
                if (!onGround) strafe()
                tickTimer.update()
            }
        }
    }

}