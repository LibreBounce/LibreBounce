/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.LiquidWalkMode
import net.ccbluex.liquidbounce.utils.block.block
import net.minecraft.init.Blocks
import net.minecraft.util.math.BlockPos

object AAC3311 : LiquidWalkMode("AAC3.3.11") {
    override fun onUpdate() {
        mc.player?.run {
            if (isSneaking) return

            if (inWater) {
                motionX *= 1.17
                motionZ *= 1.17

                if (collidingHorizontally) {
                    motionY = 0.24
                } else if (BlockPos(this).up().block != Blocks.air) {
                    motionY += 0.04
                }
            }
        }
    }
}