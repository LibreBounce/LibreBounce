/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.fly.Fly.aacFast
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode

object AAC305 : FlyMode("AAC3.0.5") {
    private var tick = 0

    override fun onUpdate() {
        mc.player?.run {
            if (tick == 2) motionY = 0.1
            else if (tick > 2) tick = 0

            if (aacFast)
                flyingSpeed = if (sidewaysSpeed == 0f) 0.08f else 0f

            tick++
        }
    }
}
