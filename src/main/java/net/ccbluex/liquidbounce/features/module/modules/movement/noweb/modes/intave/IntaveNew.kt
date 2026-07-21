/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.intave

import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.NoWebMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe

object IntaveNew : NoWebMode("IntaveNew") {
    override fun onUpdate() {
        mc.player?.run {
            if (!inCobweb)
                return

            if (isMoving && onGround && sidewaysSpeed == 0.0f) {
                if (ticksExisted % 3 == 0) {
                    strafe(0.734f)
                } else {
                    tryJump()
                    strafe(0.346f)
                }
            }
        }
    }
}
