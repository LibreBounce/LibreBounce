/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.LiquidWalkMode
import net.ccbluex.liquidbounce.utils.block.block
import net.minecraft.init.Blocks.water

object AAC : LiquidWalkMode("AAC") {
    override fun onUpdate() {
        mc.player?.run {
            if (isSneaking) return

            val blockPos = position.down()

            if (!onGround && blockPos.block == water || inWater) {
                velocityX *= 0.99999
                velocityY *= 0.0
                velocityZ *= 0.99999

                if (collidingHorizontally) velocityY =
                    ((y - (y - 1).toInt()).toInt() / 8f).toDouble()

                if (fallDistance >= 4) velocityY =
                    -0.004 else if (inWater) velocityY = 0.09
            }

            if (damagedTimer != 0) onGround = false
        }
    }
}