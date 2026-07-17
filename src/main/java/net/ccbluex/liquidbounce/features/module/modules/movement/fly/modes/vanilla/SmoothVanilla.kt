/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.vanilla

import net.ccbluex.liquidbounce.features.module.modules.movement.fly.Fly.handleVanillaKickBypass
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode

object SmoothVanilla : FlyMode("SmoothVanilla") {
    override fun onUpdate() {
        mc.player.capabilities.isFlying = true
        handleVanillaKickBypass()
    }
}
