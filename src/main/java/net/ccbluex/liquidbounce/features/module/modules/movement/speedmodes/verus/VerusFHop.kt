/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe

object VerusFHop : SpeedMode("VerusFHop") {
    override fun onMotion() {
        mc.thePlayer?.run {
            if (onGround) {
                if (movementInput.moveForward != 0f && movementInput.moveStrafe != 0f) {
                    strafe(0.4825f)
                } else {
                    strafe(0.535f)
                }
                tryJump()
            } else {
                if (movementInput.moveForward != 0f && movementInput.moveStrafe != 0f) {
                    strafe(0.334f)
                } else {
                    strafe(0.3345f)
                }
            }
        }
    }
}
