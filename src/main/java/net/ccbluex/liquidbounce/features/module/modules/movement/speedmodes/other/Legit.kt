/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object Legit : SpeedMode("Legit") {
    override fun onStrafe() {
        mc.thePlayer?.run {
            if (onGround && isMoving) tryJump()
        }
    }

    override fun onUpdate() {
        mc.thePlayer?.run {
            isSprinting = movementInput.moveForward > 0.8
        }
    }
}
