/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.LongJumpMode

object Redesky : LongJumpMode("Redesky") {
    override fun onUpdate() {
        mc.player?.run {
            flyingSpeed = 0.15f
            motionY += 0.05f
        }
    }
}