/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode

object HAC : FlyMode("HAC") {
    override fun onUpdate() {
        mc.player?.run {
            velocityX *= 0.8
            velocityZ *= 0.8
            velocityY = if (velocityY <= -0.42) 0.42 else -0.42
        }
    }
}
