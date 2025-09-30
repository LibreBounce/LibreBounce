/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe

object MiJump : SpeedMode("MiJump") {

    override fun onMotion() {
        mc.thePlayer?.run {
            if (!isMoving) return

            if (onGround && !movementInput.jump) {
                val multiplier = 1.8
                motionX *= multiplier
                motionY += 0.1
                motionZ *= multiplier
                val currentSpeed = speed
                val maxSpeed = 0.66
                if (currentSpeed > maxSpeed) {
                motionX = motionX / currentSpeed * maxSpeed
                motionZ = motionZ / currentSpeed * maxSpeed
            }
            strafe()
        }
    }
}