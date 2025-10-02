/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.ncp

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.LiquidWalkMode
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlock
import net.minecraft.block.BlockLiquid
import net.minecraft.block.material.Material
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.AxisAlignedBB.fromBounds

object NCP : LiquidWalkMode("NCP") {
    override fun onUpdate() {
        mc.thePlayer?.run {
            if (!isSneaking && collideBlock(entityBoundingBox) { it is BlockLiquid } && isInsideOfMaterial(Material.air))
                motionY = 0.08
        }
    }

    override fun onBB(event: BlockBBEvent) {
        mc.thePlayer?.run {
            if (event.block is BlockLiquid && !collideBlock(entityBoundingBox) { it is BlockLiquid } && !isSneaking) {
                event.boundingBox = fromBounds(
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

    override fun onPacket(event: PacketEvent) {
        mc.thePlayer?.run {
            if (event.packet !is C03PacketPlayer)
                return

            var nextTick = false

            if (collideBlock(
                    fromBounds(
                        entityBoundingBox.maxX,
                        entityBoundingBox.maxY,
                        entityBoundingBox.maxZ,
                        entityBoundingBox.minX,
                        entityBoundingBox.minY - 0.01,
                        entityBoundingBox.minZ
                    )
                ) { it is BlockLiquid }
            ) {
                nextTick = !nextTick
                if (nextTick) event.packet.y -= 0.001
            }
        }
    }
}