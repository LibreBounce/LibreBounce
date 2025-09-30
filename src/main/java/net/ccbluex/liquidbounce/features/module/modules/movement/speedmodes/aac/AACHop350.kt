/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object AACHop350 : SpeedMode("AACHop3.5.0") {

    // Currently not working properly, for some reason
    fun onMotion(event: MotionEvent) {
        mc.thePlayer?.run {
            if (isMoving && !isInLiquid && !isSneaking) {
                jumpMovementFactor += 0.00208f
                if (fallDistance <= 1f) {
                    if (onGround) {
                        tryJump()
                        motionX *= 1.0118f
                        motionZ *= 1.0118f
                    } else {
                        motionY -= 0.0147f
                        motionX *= 1.00138f
                        motionZ *= 1.00138f
                    }
                }
            }
        }
    }

    override fun onEnable() {
        mc.thePlayer?.run {
            if (onGround) {
                motionX = 0.0
                motionZ = 0.0
            }
        }
    }

    override fun onDisable() {
        mc.thePlayer?.jumpMovementFactor = 0.02f
    }
}