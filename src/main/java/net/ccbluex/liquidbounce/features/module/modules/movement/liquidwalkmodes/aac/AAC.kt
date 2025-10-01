/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.LiquidWalkMode
import net.minecraft.init.Blocks.water

object AAC : LiquidWalkMode("AAC") {
    override fun onUpdate() {
        mc.thePlayer?.run {
            if (isSneaking) return

            val blockPos = position.down()

            if (!onGround && blockPos.block == water || isInWater) {
                if (!isSprinting) {
                    motionX *= 0.99999
                    motionY *= 0.0
                    motionZ *= 0.99999
                    if (isCollidedHorizontally) motionY =
                        ((posY - (posY - 1).toInt()).toInt() / 8f).toDouble()
                } else {
                    motionX *= 0.99999
                    motionY *= 0.0
                    motionZ *= 0.99999
                    if (isCollidedHorizontally) motionY =
                        ((posY - (posY - 1).toInt()).toInt() / 8f).toDouble()
                }
                if (fallDistance >= 4) motionY =
                    -0.004 else if (isInWater) motionY = 0.09
            }
            if (hurtTime != 0) onGround = false
        }
    }
}