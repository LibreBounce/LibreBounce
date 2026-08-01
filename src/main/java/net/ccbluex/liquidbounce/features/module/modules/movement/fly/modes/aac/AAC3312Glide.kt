/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode

object AAC3312Glide : FlyMode("AAC3.3.12-Glide") {
    private var tick = 0

    override fun onUpdate() {
        mc.player?.run {
            if (!onGround)
                tick++

            when {
                tick == 2 -> mc.timer.tpsScale = 1f
                tick >= 12 && !onGround -> {
                    mc.timer.tpsScale = 0.1f
                    velocityY = 0.015
                    tick = 0
                }
            }
        }
    }
}
