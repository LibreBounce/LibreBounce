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

object VulcanLowHop : SpeedMode("VulcanLowHop") {
    override fun onUpdate() {
        mc.thePlayer?.run {
            if (isInLiquid || isInWeb || isOnLadder) return

            if (isMoving) {
                if (!onGround && fallDistance > 1.1) {
                    mc.timer.timerSpeed = 1f
                    motionY = -0.25
                    return
                }

                if (onGround) {
                    tryJump()
                    strafe(0.4815f)
                    mc.timer.timerSpeed = 1.263f
                } else if (ticksExisted % 4 == 0) {
                    if (ticksExisted % 3 == 0) {
                        motionY = -0.01 / motionY
                    } else {
                        motionY = -motionY / posY
                    }
                    mc.timer.timerSpeed = 0.8985f
                }

            } else {
                mc.timer.timerSpeed = 1f
            }
        }
    }
}