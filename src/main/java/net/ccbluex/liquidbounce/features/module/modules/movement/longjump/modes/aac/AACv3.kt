/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.LongJumpMode
import net.minecraft.util.math.Direction

object AACv3 : LongJumpMode("AACv3") {
    var teleported = false

    override fun onUpdate() {
        mc.player?.run {
            if (fallDistance > 0.5f && !teleported) {
                val value = 3.0
                val horizontalFacing = horizontalFacing
                var x = 0.0
                var z = 0.0

                when (horizontalFacing) {
                    Direction.NORTH -> z = -value
                    Direction.EAST -> x = value
                    Direction.SOUTH -> z = value
                    Direction.WEST -> x = -value
                    else -> {}
                }

                setPosition(posX + x, posY, posZ + z)
                teleported = true
            }
        }
    }
}