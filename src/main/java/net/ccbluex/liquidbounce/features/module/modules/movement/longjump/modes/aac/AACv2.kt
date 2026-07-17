/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.LongJumpMode
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe

object AACv2 : LongJumpMode("AACv2") {
    override fun onUpdate() {
        mc.player?.run {
            flyingSpeed = 0.09f
            motionY += 0.01320999999999999
            flyingSpeed = 0.08f
            strafe()
        }
    }
}