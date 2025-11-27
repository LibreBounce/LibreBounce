/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import kotlin.math.cos
import kotlin.math.sin

object SpectreOnGround : SpeedMode("SpectreOnGround") {
    private var speedUp = 0
    override fun onMove(event: MoveEvent) {
        mc.thePlayer?.run {
            if (!isMoving || movementInput.jump) return

            if (speedUp >= 10) {
                if (onGround) {
                    motionX = 0.0
                    motionZ = 0.0
                    speedUp = 0
                }
                return
            }

            if (onGround && mc.gameSettings.keyBindForward.isKeyDown) {
                val f = rotationYaw.toRadians()
                motionX -= sin(f) * 0.145f
                motionZ += cos(f) * 0.145f
                event.x = motionX
                event.y = 0.005
                event.z = motionZ
                speedUp++
            }
        }
    }
}