/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe

object NCPFHop : SpeedMode("NCPFHop") {
    override fun onEnable() {
        mc.timer.tpsScale = 1.0866f
        super.onEnable()
    }

    override fun onUpdate() {
        mc.player?.run {
            if (isMoving) {
                if (onGround) {
                    tryJump()
                    velocityX *= 1.01
                    velocityZ *= 1.01
                    flyingSpeed = 0.0223f
                }

                velocityY -= 0.00099999
                strafe()
            } else {
                velocityX = 0.0
                velocityZ = 0.0
            }
        }
    }
}