/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object AACHop5 : SpeedMode("AACHop5") {
    override fun onUpdate() {
        mc.thePlayer?.run {
            if (!isMoving || isInLiquid || isOnLadder || isRiding)
                return

            if (onGround) {
                tryJump()
                mc.timer.timerSpeed = 0.9385f
                speedInAir = 0.0201f
            }

            if (fallDistance < 2.5) {
                if (fallDistance > 0.7) {
                    mc.timer.timerSpeed = if (ticksExisted % 3 == 0) 1.925f
                    else { if (fallDistance < 1.25) 1.7975f }
                }

                speedInAir = 0.02f
            }

            isSprinting = movementInput.moveForward > 0.8
        }
    }
}