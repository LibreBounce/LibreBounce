/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlock
import net.minecraft.block.BlockLiquid
import net.minecraft.util.AxisAlignedBB

object ReverseStep : Module("ReverseStep", Category.MOVEMENT) {

    private val motion by float("Motion", 1f, 0.21f..4f)
    private var jumped = false

    val onUpdate = handler<UpdateEvent>(always = true) {
        mc.thePlayer?.run {
            if (onGround)
                jumped = false

            if (motionY > 0)
                jumped = true

            if (!handleEvents())
                return@handler

            if (collideBlock(entityBoundingBox) { it is BlockLiquid } ||
                collideBlock(
                    AxisAlignedBB.fromBounds(
                        entityBoundingBox.maxX,
                        entityBoundingBox.maxY,
                        entityBoundingBox.maxZ,
                        entityBoundingBox.minX,
                        entityBoundingBox.minY - 0.01,
                        entityBoundingBox.minZ
                    )
                ) {
                    it is BlockLiquid
                }) return@handler

            if (!mc.gameSettings.keyBindJump.isKeyDown && !onGround && !movementInput.jump && motionY <= 0.0 && fallDistance <= 1f && !jumped)
            motionY = (-motion).toDouble()
        }
    }

    val onJump = handler<JumpEvent>(always = true) {
        jumped = true
    }
}
