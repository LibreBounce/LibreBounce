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
import net.minecraft.init.Blocks.air
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.AxisAlignedBB
import kotlin.math.cos
import kotlin.math.sin

object WallClimb : Module("WallClimb", Category.MOVEMENT) {
    private val mode by choices("Mode", arrayOf("Simple", "CheckerClimb", "Clip", "Vulcan2.8.8", "AAC3.3.12", "AACGlide"), "Simple")
    private val clipMode by choices("ClipMode", arrayOf("Jump", "Fast"), "Fast") { mode == "Clip" }
    private val checkerClimbMotion by float("CheckerClimbMotion", 0f, 0f..1f) { mode == "CheckerClimb" }

    private var glitch = false
    private var waited = 0

    val onMove = handler<MoveEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        if (!player.isCollidedHorizontally || player.isOnLadder || player.isInLiquid)
            return@handler

        if (mode == "Simple") {
            event.y = 0.2
            player.motionY = 0.0
        }
    }

    val onUpdate = loopSequence {
        val player = mc.thePlayer ?: return@loopSequence

        when (mode) {
            "Clip" -> {
                if (player.motionY < 0)
                    glitch = true
                if (player.isCollidedHorizontally) {
                    when (clipMode) {
                        "Jump" -> if (player.onGround)
                            player.tryJump()
                        "Fast" -> if (player.onGround)
                            player.motionY = 0.42
                        else -> if (player.motionY < 0)
                            player.motionY = -0.3
                    }
                }
            }

            "CheckerClimb" -> {
                val isInsideBlock = collideBlockIntersects(player.entityBoundingBox) {
                    it != air
                }
                val motion = checkerClimbMotion

                if (isInsideBlock && motion != 0f)
                    player.motionY = motion.toDouble()
            }

            "Vulcan2.8.8" -> if (player.isCollidedHorizontally && !player.isOnLadder) {
                player.motionY = 0.0
                waitTicks(2)
                player.motionY = 9.6599696
                waitTicks(2)
                player.motionY = 0.0001
                return@loopSequence
            }

            "AAC3.3.12" -> if (player.isCollidedHorizontally && !player.isOnLadder) {
                waited++
                if (waited == 1)
                    player.motionY = 0.43
                if (waited == 12)
                    player.motionY = 0.43
                if (waited == 23)
                    player.motionY = 0.43
                if (waited == 29)
                    player.setPosition(player.posX, player.posY + 0.5, player.posZ)
                if (waited >= 30)
                    waited = 0
            } else if (player.onGround) waited = 0

            "AACGlide" -> {
                if (!player.isCollidedHorizontally || player.isOnLadder) return@loopSequence
                player.motionY = -0.19
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is C03PacketPlayer) {
            if (glitch) {
                val yaw = direction
                packet.x -= sin(yaw) * 0.00000001
                packet.z += cos(yaw) * 0.00000001
                glitch = false
            }
        }
    }

    val onBlockBB = handler<BlockBBEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        when (mode) {
            "CheckerClimb" -> if (event.y > player.posY) event.boundingBox = null
            "Clip" ->
                if (event.block == air && event.y < player.posY && player.isCollidedHorizontally
                    && !player.isOnLadder && !player.isInLiquid
                )
                    event.boundingBox = AxisAlignedBB.fromBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
                        .offset(player.posX, player.posY.toInt() - 1.0, player.posZ)
        }
    }
}