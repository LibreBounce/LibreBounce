/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.NoWebMode

object None : NoWebMode("None") {
    override fun onUpdate() {
        mc.player.isInWeb = false
    }
}
