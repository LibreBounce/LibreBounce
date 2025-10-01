/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.aac

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.LiquidWalkMode
import net.minecraft.init.Blocks.air
import net.minecraft.util.BlockPos

object AAC3311 : LiquidWalkMode("AAC3.3.11") {
    override fun onUpdate(event: UpdateEvent) {
        mc.thePlayer?.run {
            if (isSneaking) return

            if (isInWater) {
                motionX *= 1.17
                motionZ *= 1.17
                if (isCollidedHorizontally)
                    motionY = 0.24
                else if (BlockPos(mc.thePlayer).up().block != air)
                    motionY += 0.04
            }
        }
    }
}