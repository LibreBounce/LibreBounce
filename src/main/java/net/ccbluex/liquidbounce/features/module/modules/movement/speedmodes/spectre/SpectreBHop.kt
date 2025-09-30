/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe

object SpectreBHop : SpeedMode("SpectreBHop") {
    override fun onMotion() {
        mc.thePlayer?.run {
            if (!isMoving || movementInput.jump) return

            if (onGround) {
                strafe(1.1f)
                motionY = 0.44
                return
            } else {
                strafe()
            }
        }
    }
}