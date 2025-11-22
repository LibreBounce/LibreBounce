/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vulcan

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode

object Vulcan : FlyMode("Vulcan") {
    override fun onUpdate() {
        mc.thePlayer?.run {
            if (!onGround && fallDistance > 0) {
                motionY = if (ticksExisted % 2 == 0) -0.155
                else -0.1
            }
        }
    }
}
