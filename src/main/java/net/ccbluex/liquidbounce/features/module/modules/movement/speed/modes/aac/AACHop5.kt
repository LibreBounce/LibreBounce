/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object AACHop5 : SpeedMode("AACHop5") {
    override fun onUpdate() {
        mc.player?.run {
            if (!isMoving || isInLiquid || isClimbing || isRiding)
                return

            if (onGround) {
                tryJump()
                mc.timer.tpsScale = 0.9385f
                flyingSpeed = 0.0201f
            }

            if (fallDistance < 2.5) {
                if (fallDistance > 0.7) {
                    if (ticks % 3 == 0) mc.timer.tpsScale = 1.925f
                    else if (fallDistance < 1.25) mc.timer.tpsScale = 1.7975f
                }

                flyingSpeed = 0.02f
            }

            isSprinting = input.forwardSpeed > 0.8
        }
    }
}