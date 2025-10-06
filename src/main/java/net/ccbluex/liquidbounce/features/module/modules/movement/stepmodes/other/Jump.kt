/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Step.fakeJump
import net.ccbluex.liquidbounce.features.module.modules.movement.Step.jumpHeight
import net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.StepMode
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object Jump : StepMode("Jump") {

    override fun onUpdate() {
        mc.thePlayer?.run {
            if (isOnLadder || isInLiquid || isInWeb || !isMoving)
                return

            if (isCollidedHorizontally && onGround && !mc.gameSettings.keyBindJump.isKeyDown) {
                fakeJump()
                motionY = jumpHeight.toDouble()
            }
        }
    }
}