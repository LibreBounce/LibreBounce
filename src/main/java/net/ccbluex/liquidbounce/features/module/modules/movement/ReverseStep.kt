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
import net.minecraft.block.LiquidBlock
import net.minecraft.util.math.Box

object ReverseStep : Module("ReverseStep", Category.MOVEMENT) {

    private val motion by float("Motion", 1f, 0.21f..4f)
    private var jumped = false

    val onUpdate = handler<UpdateEvent>(always = true) {
        mc.player?.run {
            if (onGround)
                jumped = false

            if (motionY > 0)
                jumped = true

            if (!handleEvents())
                return@handler

            if (collideBlock(shape) { it is LiquidBlock } ||
                collideBlock(
                    Box.fromBounds(
                        shape.maxX,
                        shape.maxY,
                        shape.maxZ,
                        shape.minX,
                        shape.minY - 0.01,
                        shape.minZ
                    )
                ) {
                    it is LiquidBlock
                }) return@handler

            if (!mc.gameOptions.jumpKey.isKeyDown && !onGround && !input.jump && motionY <= 0.0 && fallDistance <= 1f && !jumped)
                motionY = -motion.toDouble()
        }
    }

    val onJump = handler<JumpEvent>(always = true) {
        jumped = true
    }
}
