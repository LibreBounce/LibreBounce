/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.matrix

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.matrixLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.extraGroundBoost
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe

/*
* Working on Matrix: 7.11.8
* Tested on: eu.loyisa.cn & anticheat.com
* Credit: @EclipsesDev
*/
object MatrixHop : SpeedMode("MatrixHop") {

    override fun onUpdate() {
        mc.thePlayer?.run {
            if (isInLiquid || isInWeb || isOnLadder) return

            if (matrixLowHop) jumpMovementFactor = 0.026f

            if (isMoving) {
                if (onGround) {
                    strafe(if (!Scaffold.handleEvents()) speed + extraGroundBoost else speed)
                    motionY = 0.42 - if (matrixLowHop) 3.48E-3 else 0.0
                } else {
                    if (!Scaffold.handleEvents() && speed < 0.19) {
                        strafe()
                    }
                }

                if (fallDistance <= 0.4 && moveStrafing == 0f) {
                    speedInAir = 0.02035f
                } else {
                    speedInAir = 0.02f
                }
            }
        }
    }
}
