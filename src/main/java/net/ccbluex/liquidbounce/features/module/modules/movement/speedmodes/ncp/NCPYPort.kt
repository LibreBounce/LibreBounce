/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import kotlin.math.cos
import kotlin.math.sin

object NCPYPort : SpeedMode("NCPYPort") {
    private var jumps = 0

    override fun onMotion() {
        mc.thePlayer?.run {
            if (isOnLadder || isInLiquid || isInWeb || !isMoving || isInWater) return

            if (jumps >= 4 && onGround) jumps = 0

            if (onGround) {
                motionY = if (jumps <= 1) 0.42 else 0.4
                val f = rotationYaw.toRadians()
                motionX -= sin(f) * 0.2f
                motionZ += cos(f) * 0.2f
                jumps++
            } else {
                if (jumps <= 1) motionY = -5.0
            }
            strafe()
        }
    }
}