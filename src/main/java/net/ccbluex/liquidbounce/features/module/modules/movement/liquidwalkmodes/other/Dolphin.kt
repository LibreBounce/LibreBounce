/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.other

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.LiquidWalkMode

object Dolphin : LiquidWalkMode("Dolphin") {
    override fun onUpdate() {
        mc.thePlayer?.run {
            if (isSneaking) return

            if (isInWater) motionY += 0.03999999910593033
        }
    }
}