/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object OnGround : SpeedMode("OnGround") {
    override fun onMotion() {
        mc.thePlayer?.run {
            if (isInWater || isOnLadder || isCollidedHorizontally)
                return

            if (!isMoving || fallDistance > 3.994) return

            posY -= 0.3993000090122223
            motionY = -1000.0
            cameraPitch = 0.3f
            distanceWalkedModified = 44f
            mc.timer.timerSpeed = 1f

            if (onGround) {
                posY += 0.3993000090122223
                motionY = 0.3993000090122223
                distanceWalkedOnStepModified = 44f
                motionX *= 1.590000033378601
                motionZ *= 1.590000033378601
                cameraPitch = 0f
                mc.timer.timerSpeed = 1.199f
            }
        }
    }
}