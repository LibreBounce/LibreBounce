/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.ncp

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.LiquidWalkMode
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlock
import net.minecraft.block.BlockLiquid
import net.minecraft.block.material.Material
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.Box.fromBounds

object NCP : LiquidWalkMode("NCP") {
    private var nextTick = false

    override fun onUpdate() {
        mc.player?.run {
            if (!isSneaking && collideBlock(shape) { it is BlockLiquid } && isInsideOfMaterial(Material.air))
                motionY = 0.08
        }
    }

    override fun onBB(event: BlockBBEvent) {
        mc.player?.run {
            if (event.block is BlockLiquid && !collideBlock(shape) { it is BlockLiquid } && !isSneaking) {
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
        mc.player?.run {
            if (event.packet !is PlayerMoveC2SPacket)
                return

            if (collideBlock(
                    fromBounds(
                        shape.maxX,
                        shape.maxY,
                        shape.maxZ,
                        shape.minX,
                        shape.minY - 0.01,
                        shape.minZ
                    )
                ) { it is BlockLiquid }
            ) {
                nextTick = !nextTick
                if (nextTick) event.packet.y -= 0.001
            }
        }
    }
}