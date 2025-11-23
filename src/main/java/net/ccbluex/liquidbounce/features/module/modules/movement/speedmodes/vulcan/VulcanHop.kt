/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe

object VulcanHop : SpeedMode("VulcanHop") {
    override fun onUpdate() {
        mc.thePlayer?.run {
            if (isInLiquid || isInWeb || isOnLadder) return

            if (isMoving) {
                if (isAirBorne && fallDistance > 2) {
                    mc.timer.timerSpeed = 1f
                    return
                }

                if (onGround) {
                    tryJump()

                    if (motionY > 0) 
                        mc.timer.timerSpeed = 1.1453f

                    strafe(0.4815f)
                } else if (motionY < 0) mc.timer.timerSpeed = 0.9185f
            } else mc.timer.timerSpeed = 1f
        }
    }
}