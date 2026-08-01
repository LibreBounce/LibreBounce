/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import kotlin.math.cos
import kotlin.math.sin

object NCPYPort : SpeedMode("NCPYPort") {
    private var jumps = 0

    override fun onMotion() {
        mc.player?.run {
            if (isClimbing || isInLiquid || inCobweb || !isMoving || inWater) return

            if (jumps >= 4 && onGround) jumps = 0

            if (onGround) {
                val f = yaw.toRadians()

                velocityX -= sin(f) * 0.2f
                velocityY = if (jumps <= 1) 0.42 else 0.4
                velocityZ += cos(f) * 0.2f
                jumps++
            } else {
                if (jumps <= 1) velocityY = -5.0
            }
            strafe()
        }
    }
}