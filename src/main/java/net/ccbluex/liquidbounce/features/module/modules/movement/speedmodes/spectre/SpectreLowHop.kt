/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe

object SpectreLowHop : SpeedMode("SpectreLowHop") {
    override fun onMotion() {
        val player = mc.thePlayer ?: return

        if (!player.isMoving || player.movementInput.jump) return

        if (player.onGround) {
            strafe(1.1f)
            player.motionY = 0.15
            return
        }
        strafe()
    }

}