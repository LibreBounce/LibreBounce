/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.other

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.LiquidWalkMode
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlock
import net.minecraft.block.BlockLiquid
import net.minecraft.block.material.Material.air
import net.minecraft.util.AxisAlignedBB

object Vanilla : LiquidWalkMode("Vanilla") {
    override fun onUpdate(event: UpdateEvent) {
        mc.thePlayer?.run {
            if (isSneaking) return

            if (collideBlock(entityBoundingBox) { it is BlockLiquid } && isInsideOfMaterial(air)) motionY = 0.08
        }
    }

    override fun onBlockBB(event: BlockBBEvent) {
        mc.thePlayer?.run {
            if (event.block is BlockLiquid && !collideBlock(player.entityBoundingBox) { it is BlockLiquid } && !player.isSneaking) {
                event.boundingBox = AxisAlignedBB.fromBounds(
                    event.x.toDouble(),
                    event.y.toDouble(),
                    event.z.toDouble(),
                    event.x + 1.toDouble(),
                    event.y + 1.toDouble(),
                    event.z + 1.toDouble()
                )
            }
        }
    }
}