/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac

import net.ccbluex.liquidbounce.event.EventManager.call
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.block.BlockCarpet
import kotlin.math.cos
import kotlin.math.sin

object AACHop3313 : SpeedMode("AACHop3.3.13") {
    override fun onUpdate() {
        mc.thePlayer?.run {
            if (!isMoving || isInLiquid || isOnLadder || isRiding || hurtTime > 0) return

            if (onGround && isCollidedVertically) {
                val yawRad = rotationYaw.toRadians()
                motionX -= sin(yawRad) * 0.202f
                motionZ += cos(yawRad) * 0.202f
                motionY = 0.405
                call(JumpEvent(0.405f, EventState.PRE))
                strafe()
            } else if (fallDistance < 0.31f) {
                if (position.block is BlockCarpet) // why?
                    return

                // Motion XZ
                jumpMovementFactor = if (moveStrafing == 0f) 0.027f else 0.021f
                motionX *= 1.001
                motionZ *= 1.001

                // Motion Y
                if (!isCollidedHorizontally) motionY -= 0.014999993f
            } else {
                jumpMovementFactor = 0.02f
            }
        }
    }

    override fun onDisable() {
        mc.thePlayer.jumpMovementFactor = 0.02f
    }
}