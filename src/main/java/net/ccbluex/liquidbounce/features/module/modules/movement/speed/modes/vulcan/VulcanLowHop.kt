/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.vulcan

import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe

object VulcanLowHop : SpeedMode("VulcanLowHop") {
    override fun onUpdate() {
        mc.player?.run {
            if (isInLiquid || inCobweb || isClimbing) return

            if (isMoving) {
                if (!onGround && fallDistance > 1.1) {
                    mc.timer.tpsScale = 1f
                    velocityY = -0.25
                    return
                }

                if (onGround) {
                    tryJump()
                    strafe(0.4815f)
                    mc.timer.tpsScale = 1.263f
                } else if (ticks % 4 == 0) {
                    velocityY = if (ticks % 3 == 0) -0.01 / velocityY
                    else -velocityY / y

                    mc.timer.tpsScale = 0.8985f
                }

            } else mc.timer.tpsScale = 1f
        }
    }
}