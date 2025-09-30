/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode

object Hycraft : LongJumpMode("Hycraft") {
    override fun onUpdate() {
        mc.thePlayer?.run {
            if (motionY < 0) {
                motionY *= 0.75f
                jumpMovementFactor = 0.055f
            } else {
                motionY += 0.02f
                jumpMovementFactor = 0.08f
            }
        }
    }
}