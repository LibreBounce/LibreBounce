/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.ncp

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Step.fakeJump
import net.ccbluex.liquidbounce.features.module.modules.movement.Step.couldStep
import net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.StepMode
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.direction
import kotlin.math.cos
import kotlin.math.sin

object MotionNCP : StepMode("MotionNCP") {
    private var ncpNextStep = 0

    override fun onMove(event: MoveEvent) {
        val player = mc.thePlayer ?: return

        if (!player.isCollidedHorizontally || mc.gameSettings.keyBindJump.isKeyDown)
            return

        // Motion steps
        when {
            player.onGround && couldStep() -> {
                fakeJump()
                player.motionY = 0.0
                event.y = 0.41999998688698
                ncpNextStep = 1
            }

            ncpNextStep == 1 -> {
                event.y = 0.7531999805212 - 0.41999998688698
                ncpNextStep = 2
            }

            ncpNextStep == 2 -> {
                event.y = 1.001335979112147 - 0.7531999805212
                event.x = -sin(direction) * 0.7
                event.z = cos(direction) * 0.7

                ncpNextStep = 0
            }
        }
    }
}