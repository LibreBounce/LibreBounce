/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.LiquidWalkMode
import net.minecraft.block.BlockLiquid
import net.minecraft.util.BlockPos

object Spartan : LiquidWalkMode("Spartan") {
    override fun onUpdate() {
        mc.thePlayer?.run {
            if (isSneaking) return

            if (isInWater) {
                if (isCollidedHorizontally) {
                    motionY += 0.15
                    return
                }
                val block = BlockPos(mc.thePlayer).up().block
                val blockUp = BlockPos(posX, posY + 1.1, posZ).block

                if (blockUp is BlockLiquid) {
                    motionY = 0.1
                } else if (block is BlockLiquid) {
                    motionY = 0.0
                }

                onGround = true
                motionX *= 1.085
                motionZ *= 1.085
            }
        }
    }
}