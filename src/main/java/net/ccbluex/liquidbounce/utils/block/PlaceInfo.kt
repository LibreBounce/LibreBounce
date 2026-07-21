/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.block

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d


class PlaceInfo(val blockPos: BlockPos, val enumFacing: Direction, var vec3: Vec3 = blockPos.center) {

    companion object {

        /**
         * Allows you to find a specific place info for your [blockPos]
         */
        fun get(pos: BlockPos) = Direction.entries.find {
            it != Direction.UP && pos.offset(it).canBeClicked()
        }?.let { side -> PlaceInfo(pos.offset(side), side.opposite) }
    }
}