/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode

object AAC3312Glide : FlyMode("AAC3.3.12-Glide") {
    private var tick = 0

    override fun onUpdate() {
        mc.thePlayer?.run {
            if (!onGround)
                tick++

            if (tick == 2) mc.timer.timerSpeed = 1f
            else if (tick == 12) mc.timer.timerSpeed = 0.1f
            else if (tick >= 12 && !onGround) {
                tick = 0
                motionY = .015
            }
        }
    }
}
