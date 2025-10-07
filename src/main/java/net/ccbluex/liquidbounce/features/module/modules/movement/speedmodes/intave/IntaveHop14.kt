/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.boost
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.intaveLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.strafeStrength
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.initialBoostMultiplier
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.groundTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.airTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe

/*
* Working on Intave: 14
* Tested on: mc.mineblaze.net
* Credit: @thatonecoder & @larryngton / Intave14
*/
object IntaveHop14 : SpeedMode("IntaveHop14") {

    private const val boost_constant = 0.003

    override fun onUpdate() {
        mc.thePlayer?.run {
            if (!isMoving || isInLiquid || isInWeb || isOnLadder) return

            if (onGround) {
                motionY = 0.42 - if (intaveLowHop) 1.7E-14 else 0.0

                if (isSprinting) strafe(strength = strafeStrength.toDouble())

                mc.timer.timerSpeed = groundTimer
            } else {
                mc.timer.timerSpeed = airTimer
            }

            if (boost && motionY > 0.003 && isSprinting) {
                motionX *= 1f + (boost_constant * initialBoostMultiplier)
                motionZ *= 1f + (boost_constant * initialBoostMultiplier)
            }
        }
    }
}
