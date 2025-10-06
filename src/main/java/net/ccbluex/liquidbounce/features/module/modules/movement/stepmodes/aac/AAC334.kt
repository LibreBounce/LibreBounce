/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.Step.fakeJump
import net.ccbluex.liquidbounce.features.module.modules.movement.Step.couldStep
import net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.StepMode
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object AAC334 : StepMode("AAC3.3.4") {
    private var isAACStep = false

    override fun onUpdate() {
        mc.thePlayer?.run {
            if (isOnLadder || isInLiquid || isInWeb)
                return

            if (isCollidedHorizontally && isMoving) {
                if (onGround && couldStep()) {
                    motionX *= 1.26
                    motionZ *= 1.26
                    tryJump()
                    isAACStep = true
                }

                if (isAACStep) {
                    motionY -= 0.015

                    if (!isUsingItem && movementInput.moveStrafe == 0F)
                        jumpMovementFactor = 0.3F
                    }
            } else {
                isAACStep = false
            }
        }
    }
}