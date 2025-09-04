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
        val player = mc.thePlayer ?: return

        if (!player.isMoving || player.isInLiquid ||
            player.isOnLadder || player.isRiding || player.hurtTime > 0
        ) return
        if (player.onGround && player.isCollidedVertically) {
            // MotionXYZ
            val yawRad = player.rotationYaw.toRadians()
            player.motionX -= sin(yawRad) * 0.202f
            player.motionZ += cos(yawRad) * 0.202f
            player.motionY = 0.405
            call(JumpEvent(0.405f, EventState.PRE))
            strafe()
        } else if (player.fallDistance < 0.31f) {
            if (player.position.block is BlockCarpet) // why?
                return

            // Motion XZ
            player.jumpMovementFactor = if (player.moveStrafing == 0f) 0.027f else 0.021f
            player.motionX *= 1.001
            player.motionZ *= 1.001

            // Motion Y
            if (!player.isCollidedHorizontally) player.motionY -= 0.014999993f
        } else player.jumpMovementFactor = 0.02f
    }

    override fun onDisable() {
        mc.thePlayer.jumpMovementFactor = 0.02f
    }
}