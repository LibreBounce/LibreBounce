/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.event.async.loopSequence
import net.ccbluex.liquidbounce.event.async.waitTicks
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlockIntersects
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.direction
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.AxisAlignedBB
import kotlin.math.cos
import kotlin.math.sin

object WallClimb : Module("WallClimb", Category.MOVEMENT) {
    private val mode by choices("Mode", arrayOf("Simple", "CheckerClimb", "Clip", "AAC3.3.12", "AACGlide"), "Simple")
    private val clipMode by choices("ClipMode", arrayOf("Jump", "Fast"), "Fast") { mode == "Clip" }
    private val checkerClimbMotion by float("CheckerClimbMotion", 0f, 0f..1f) { mode == "CheckerClimb" }

    private var glitch = false
    private var waited = 0

    val onMove = handler<MoveEvent> { event ->
        mc.thePlayer?.run {
            if (!isCollidedHorizontally || isOnLadder || isInLiquid)
                return@handler

            if (mode == "Simple") {
                event.y = 0.2
                motionY = 0.0
            }
        }
    }

    val onUpdate = loopSequence {
        mc.thePlayer?.run {
            when (mode) {
                "Clip" -> {
                    if (motionY < 0)
                        glitch = true

                    if (isCollidedHorizontally) {
                        when (clipMode) {
                            "Jump" -> if (onGround)
                                tryJump()
                            "Fast" -> if (onGround)
                                motionY = 0.42
                            else -> if (motionY < 0)
                                motionY = -0.3
                        }
                    }
                }

                "CheckerClimb" -> {
                    val isInsideBlock = collideBlockIntersects(entityBoundingBox) {
                        it != Blocks.air
                    }

                    val motion = checkerClimbMotion

                    if (isInsideBlock && motion != 0f)
                        motionY = motion.toDouble()
                }

                "AAC3.3.12" -> if (isCollidedHorizontally && !isOnLadder) {
                    when (++waited) {
                        1, 12, 23 -> motionY = 0.43
                        29 -> setPosition(posX, posY + 0.5, posZ)
                        30 -> waited = 0
                    }
                } else if (onGround) waited = 0

                "AACGlide" -> if (isCollidedHorizontally && !isOnLadder) motionY = -0.19
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is C03PacketPlayer && glitch) {
            val yaw = direction

            packet.x -= sin(yaw) * 0.00000001
            packet.z += cos(yaw) * 0.00000001
            glitch = false
        }
    }

    val onBlockBB = handler<BlockBBEvent> { event ->
        mc.thePlayer?.run {
            when (mode) {
                "CheckerClimb" -> if (event.y > posY) event.boundingBox = null

                "Clip" ->
                    if (event.block == Blocks.air && event.y < posY && isCollidedHorizontally
                        && !isOnLadder && !isInLiquid
                    )
                        event.boundingBox = AxisAlignedBB.fromBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
                            .offset(posX, posY.toInt() - 1.0, posZ)
            }
        }
    }
}