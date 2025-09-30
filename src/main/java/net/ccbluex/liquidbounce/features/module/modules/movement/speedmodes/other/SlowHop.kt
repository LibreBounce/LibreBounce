/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.speed

object SlowHop : SpeedMode("SlowHop") {
    override fun onMotion() {
        mc.thePlayer?.run {
            if (isInLiquid || isInWeb || isOnLadder) return

            if (isMoving) {
                if (onGround) tryJump() else speed *= 1.011f
            } else {
                motionX = 0.0
                motionZ = 0.0
            }
        }
    }
}