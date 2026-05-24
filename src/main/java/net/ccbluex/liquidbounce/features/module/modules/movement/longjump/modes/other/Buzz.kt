/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.LongJumpMode
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.speed

object Buzz : LongJumpMode("Buzz") {
    override fun onUpdate() {
        mc.thePlayer.motionY += 0.4679942989799998
        speed *= 0.7578698f
    }
}
