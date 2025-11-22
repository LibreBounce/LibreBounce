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

object NCPFHop : SpeedMode("NCPFHop") {
    override fun onEnable() {
        mc.timer.timerSpeed = 1.0866f
        super.onEnable()
    }

    override fun onUpdate() {
        mc.thePlayer?.run {
            if (isMoving) {
                if (onGround) {
                    tryJump()
                    motionX *= 1.01
                    motionZ *= 1.01
                    speedInAir = 0.0223f
                }

                motionY -= 0.00099999
                strafe()
            } else {
                motionX = 0.0
                motionZ = 0.0
            }
        }
    }
}