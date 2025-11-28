/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.block.BlockStairs
import net.minecraft.util.BlockPos

object FastStairs : Module("FastStairs", Category.MOVEMENT) {

    private val mode by choices("Mode", arrayOf("Step", "NCP", "AAC3.1.0", "AAC3.3.6", "AAC3.3.13"), "NCP")
    private val longJump by boolean("LongJump", false) { mode.startsWith("AAC") }

    private var canJump = false

    private var walkingDown = false

    val onUpdate = handler<UpdateEvent> {
        mc.thePlayer?.run {
            if (!isMoving || Speed.handleEvents())
                return@handler

            when {
                fallDistance > 0 && !walkingDown -> walkingDown = true
                posY > prevChasingPosY -> walkingDown = false
            }


            if (!onGround)
                return@handler

            val blockPos = BlockPos(this)

            if (blockPos.block is BlockStairs && !walkingDown) {
                setPosition(posX, posY + 0.5, posZ)

                val motion = when (mode) {
                    "NCP" -> 1.4
                    "AAC3.1.0" -> 1.5
                    "AAC3.3.13" -> 1.2
                    else -> 1.0
                }

                motionX *= motion
                motionZ *= motion
            }

            if (blockPos.down().block is BlockStairs) {
                if (walkingDown) {
                    when (mode) {
                        "NCP" -> motionY = -1.0
                        "AAC3.3.13" -> motionY -= 0.014
                    }

                    return@handler
                }

                val motion = when (mode) {
                    "AAC3.3.6" -> 1.48
                    "AAC3.3.13" -> 1.52
                    else -> 1.3
                }

                motionX *= motion
                motionZ *= motion
                canJump = true
            } else if (mode.startsWith("AAC") && canJump) {
                if (longJump) {
                    tryJump()
                    motionX *= 1.35
                    motionZ *= 1.35
                }

                canJump = false
            }
        }
    }

    override val tag
        get() = mode
}