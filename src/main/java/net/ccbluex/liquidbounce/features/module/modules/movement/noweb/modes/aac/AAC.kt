/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.NoWebMode

object AAC : NoWebMode("AAC") {
    override fun onUpdate() {
        if (!mc.player.inCobweb) {
            return
        }

        mc.player.flyingSpeed = 0.59f

        if (!mc.gameOptions.sneakKey.isKeyDown)
            mc.player.motionY = 0.0
    }
}
