/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.intave

import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.NoWebMode

object IntaveOld : NoWebMode("IntaveOld") {
    override fun onUpdate() {
        mc.thePlayer?.run {
            if (!isInWeb)
                return

            if (movementInput.moveStrafe == 0.0F && mc.gameSettings.keyBindForward.isKeyDown && isCollidedVertically) {
                jumpMovementFactor = 0.74F
            } else {
                jumpMovementFactor = 0.2F
                onGround = true
            }
        }
    }
}
