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
        val player = mc.thePlayer ?: return

        if (player.isOnLadder || player.isInLiquid || player.isInWeb)
            return

        if (player.isCollidedHorizontally && player.isMoving) {
            if (player.onGround && couldStep()) {
                player.motionX *= 1.26
                player.motionZ *= 1.26
                player.tryJump()
                isAACStep = true
            }

            if (isAACStep) {
                player.motionY -= 0.015

                if (!player.isUsingItem && player.movementInput.moveStrafe == 0F)
                    player.jumpMovementFactor = 0.3F
                }
        } else {
            isAACStep = false
        }
}