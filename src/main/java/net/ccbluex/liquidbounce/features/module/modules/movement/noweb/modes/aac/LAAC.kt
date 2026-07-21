/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.NoWebMode
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object LAAC : NoWebMode("LAAC") {
    override fun onUpdate() {
        mc.player?.run {
            if (!inCobweb)
                return

            flyingSpeed = if (input.moveStrafe != 0f) 1f else 1.21f

            if (!mc.gameOptions.sneakKey.isKeyDown)
                motionY = 0.0

            if (onGround)
                tryJump()
        }
    }
}
