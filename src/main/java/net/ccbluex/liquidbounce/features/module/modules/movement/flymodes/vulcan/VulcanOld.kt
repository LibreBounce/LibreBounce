/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vulcan

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode

object VulcanOld : FlyMode("VulcanOld") {
    override fun onUpdate() {
        mc.thePlayer?.run {
            if (!onGround && fallDistance > 0) {
                if (ticksExisted % 2 == 0) {
                    motionY = -0.1
                    jumpMovementFactor = 0.0265f
                } else {
                    motionY = -0.16
                    jumpMovementFactor = 0.0265f
                }
            }
        }
    }
}
