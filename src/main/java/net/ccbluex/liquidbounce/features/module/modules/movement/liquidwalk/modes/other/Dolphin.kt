/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.LiquidWalkMode

object Dolphin : LiquidWalkMode("Dolphin") {
    override fun onUpdate() {
        mc.player?.run {
            if (isSneaking) return

            if (inWater) motionY += 0.03999999910593033
        }
    }
}