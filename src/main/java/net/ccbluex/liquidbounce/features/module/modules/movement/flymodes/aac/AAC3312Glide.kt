/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode

object AAC3312Glide : FlyMode("AAG3.3.12-Glide") {
    private var tick = 0

    override fun onUpdate() {
        mc.thePlayer?.run {
            if (!onGround)
                tick++

            when (tick) {
                2 -> mc.timer.timerSpeed = 1f
                in 12..13 && !onGround -> {
                    mc.timer.timerSpeed = 0.1f
                    tick = 0
                    motionY = 0.015
                }
            }
        }
    }
}
