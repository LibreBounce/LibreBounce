/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.potion.Potion

object UNCPHop : SpeedMode("UNCPHop") {
    private var speed = 0.0f
    private var tick = 0

    override fun onUpdate() {
        mc.thePlayer?.run {
            if (isInLiquid || isInWeb || isOnLadder) return

            if (isMoving) {
                if (onGround) {
                    speed = if (isPotionActive(Potion.moveSpeed)
                        && getActivePotionEffect(Potion.moveSpeed).amplifier >= 1
                    ) 0.4563f else 0.3385f

                    tryJump()
                } else {
                    speed *= 0.98f
                }

                if (isAirBorne && fallDistance > 2) {
                    mc.timer.timerSpeed = 1f
                    return
                }

                strafe(speed, false)

                if (!onGround && ++tick % 3 == 0) {
                    mc.timer.timerSpeed = 1.0815f
                    tick = 0
                } else {
                    mc.timer.timerSpeed = 0.9598f
                }
            } else {
                mc.timer.timerSpeed = 1f
            }
        }
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }
}