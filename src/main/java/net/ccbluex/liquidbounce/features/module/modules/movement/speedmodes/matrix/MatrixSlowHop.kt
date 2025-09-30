/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.matrix

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe

/*
* Working on Matrix: 7.11.8
* Tested on: eu.loyisa.cn & anticheat.com
* Credit: @EclipsesDev
*/
object MatrixSlowHop : SpeedMode("MatrixSlowHop") {

    override fun onUpdate() {
        mc.thePlayer?.run {
            if (isInLiquid || isInWeb || isOnLadder) return

            if (isMoving) {
                if (!onGround && fallDistance > 2) {
                    mc.timer.timerSpeed = 1f
                    return
                }

                if (onGround) {
                    motionY = 0.42 - if (Speed.matrixLowHop) 3.48E-3 else 0.0
                    mc.timer.timerSpeed = 0.5195f
                    strafe(speed + Speed.extraGroundBoost)
                } else {
                    mc.timer.timerSpeed = 1.0973f
                }

                if (fallDistance <= 0.4 && moveStrafing == 0f) {
                    speedInAir = 0.02035f
                } else {
                    speedInAir = 0.02f
                }
            } else {
                mc.timer.timerSpeed = 1f
            }
        }
    }
}
