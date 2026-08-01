/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object OnGround : SpeedMode("OnGround") {
    private const val horizontal = 1.590000033378601
    private const val vertical = 0.3993000090122223

    override fun onMotion() {
        mc.player?.run {
            if (inWater || isClimbing || collidingHorizontally)
                return

            if (!isMoving || fallDistance > 3.994) return

            y -= vertical
            velocityY = -1000.0
            tilt = 0.3f
            distanceWalkedModified = 44f
            mc.timer.tpsScale = 1f

            if (onGround) {
                y += vertical
                velocityY = vertical
                distanceWalkedOnStepModified = 44f
                velocityX *= horizontal
                velocityZ *= horizontal
                tilt = 0f
                mc.timer.tpsScale = 1.199f
            }
        }
    }
}