/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.aac

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.LiquidWalkMode.aacFly
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.LiquidWalkMode

object AACFly : LiquidWalkMode("AACFly") {
    override fun onMove(event: MoveEvent) {
        mc.thePlayer?.run {
            if (isInWater) {
                event.y = aacFly.toDouble()
                motionY = aacFly.toDouble()
            }
        }
    }
}