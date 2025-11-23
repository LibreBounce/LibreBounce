/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.verus

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe

/*
* Working on Verus: b3896/b3901
* Tested on: eu.loyisa.cn, anticheat-test.com
*/
object VerusGlide : FlyMode("VerusGlide") {

    override fun onUpdate() {
        mc.thePlayer?.run {
            if (isInLiquid || isInWeb || isOnLadder) return

            if (!onGround && fallDistance > 1) {
                // Good job, Verus
                motionY = -0.09800000190734863

                if (movementInput.moveForward != 0f && moveStrafing != 0f) strafe(0.334f)
                else strafe(0.3345f)
            }
        }
    }
}
