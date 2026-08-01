/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.LiquidWalkMode
import net.ccbluex.liquidbounce.utils.block.block
import net.minecraft.block.LiquidBlock
import net.minecraft.util.math.BlockPos

object Spartan : LiquidWalkMode("Spartan") {
    override fun onUpdate() {
        mc.player?.run {
            if (isSneaking) return

            if (inWater) {
                if (collidingHorizontally) {
                    velocityY += 0.15
                    return
                }

                val block = BlockPos(this).up().block
                val blockUp = BlockPos(x, y + 1.1, z).block

                if (blockUp is LiquidBlock) {
                    velocityY = 0.1
                } else if (block is LiquidBlock) {
                    velocityY = 0.0
                }

                onGround = true
                velocityX *= 1.085
                velocityZ *= 1.085
            }
        }
    }
}