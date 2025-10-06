/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.Step.fakeJump
import net.ccbluex.liquidbounce.features.module.modules.movement.Step.isStep
import net.ccbluex.liquidbounce.features.module.modules.movement.Step.timer
import net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.StepMode
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.direction
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import kotlin.math.cos
import kotlin.math.sin

object LAAC : StepMode("LAAC") {

    override fun onUpdate() {
        mc.thePlayer?.run {
            if (isOnLadder || isInLiquid || isInWeb || !isMoving)
                return

            if (isCollidedHorizontally && onGround) {
                isStep = true

                fakeJump()

                motionY += 0.620000001490116
                motionX -= sin(direction) * 0.2
                motionZ += cos(direction) * 0.2

                timer.reset()

                onGround = true
            } else {
                isStep = false
            }
        }
    }
}