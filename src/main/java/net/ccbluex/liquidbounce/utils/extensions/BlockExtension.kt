/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.utils.MinecraftInstance.Companion.mc
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isBlockBBValid
import net.minecraft.block.*
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3

val BlockPos.state: IBlockState?
    get() = mc.theWorld?.getBlockState(this)

val BlockPos.block: Block?
    get() = this.state?.block

val BlockPos.material: Material?
    get() = this.block?.material

val BlockPos.isReplaceable: Boolean
    get() = this.material?.isReplaceable ?: false

val BlockPos.center: Vec3
    get() = Vec3(x + 0.5, y + 0.5, z + 0.5)

fun BlockPos.toVec() = Vec3(this)

fun BlockPos.canBeClicked(): Boolean {
    val state = this.state ?: return false
    val block = state.block ?: return false

    return when {
        this !in mc.theWorld.worldBorder -> false
        !block.canCollideCheck(state, false) -> false
        block.material.isReplaceable -> false
        block.hasTileEntity(state) -> false
        !isBlockBBValid(this, state, supportSlabs = true, supportPartialBlocks = true) -> false
        mc.theWorld.loadedEntityList.any { it is EntityFallingBlock && it.position == this } -> false
        block is BlockContainer || block is BlockWorkbench -> false
        else -> true
    }
}
