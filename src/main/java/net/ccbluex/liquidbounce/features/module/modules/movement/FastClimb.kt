/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlockIntersects
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.block.LadderBlock
import net.minecraft.block.VineBlock
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Position
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

object FastClimb : Module("FastClimb", Category.MOVEMENT) {

    val mode by choices(
        "Mode",
        arrayOf("Vanilla", "Delay", "Clip", "AAC3.0.0", "AAC3.0.5", "SAAC3.1.2", "AAC3.1.2"), "Vanilla"
    )
    private val speed by float("Speed", 1F, 0.01F..5F) { mode == "Vanilla" }

    // Delay mode | Separated Vanilla & Delay speed value
    private val climbSpeed by float("ClimbSpeed", 1f, 0.01f..5f) { mode == "Delay" }
    private val tickDelay by int("TickDelay", 10, 1..20) { mode == "Delay" }

    private val climbDelay = tickDelay
    private var climbCount = 0

    private fun playerClimb() {
        mc.player?.run {
            velocityY = 0.0
            inCobweb = true
            onGround = true

            inCobweb = false
        }
    }

    val onMove = handler<MoveEvent> { event ->
        mc.player?.run {
            when {
                mode == "AAC3.0.0" && collidingHorizontally -> {
                    var x = 0.0
                    var z = 0.0

                    when (horizontalFacing) {
                        Direction.NORTH -> z = -0.99
                        Direction.EAST -> x = 0.99
                        Direction.SOUTH -> z = 0.99
                        Direction.WEST -> x = -0.99
                        else -> {}
                    }

                    val block = BlockPos(x + x, y, z + z).block

                    if (block is LadderBlock || block is VineBlock) {
                        event.y = 0.5
                        velocityY = 0.0
                    }
                }

                mode == "AAC3.0.5" && mc.options.forwardKey.isPressed &&
                    collideBlockIntersects(shape) {
                        it is LadderBlock || it is VineBlock
                    } -> {
                    event.x = 0.0
                    event.y = 0.5
                    event.z = 0.0

                    velocityX = 0.0
                    velocityY = 0.0
                    velocityZ = 0.0
                }

                mode == "Clip" && isClimbing && mc.options.forwardKey.isPressed -> {
                    for (i in y.toInt()..y.toInt() + 8) {
                        val block = BlockPos(x, i.toDouble(), z).block

                        if (block !is LadderBlock) {
                            var x = 0.0
                            var z = 0.0

                            when (horizontalFacing) {
                                Direction.NORTH -> z = -1.0
                                Direction.EAST -> x = 1.0
                                Direction.SOUTH -> z = 1.0
                                Direction.WEST -> x = -1.0
                                else -> {}
                            }

                            setPosition(x + x, i.toDouble(), z + z)
                            break
                        } else {
                            setPosition(x, i.toDouble(), z)
                        }
                    }
                }
            }

            if (collidingHorizontally && isClimbing) {
                when (mode) {
                    "Vanilla" -> {
                        event.y = speed.toDouble()
                        velocityY = 0.0
                    }

                    "Delay" -> {
                        if (climbCount >= climbDelay) {
                            event.y = climbSpeed.toDouble()
                            playerClimb()

                            sendPacket(Position(x, y, z, true))
                            climbCount = 0
                        } else {
                            y = prevPosY

                            playerClimb()
                            climbCount++

                        }
                    }

                    "SAAC3.1.2" -> {
                        event.y = 0.1649
                        velocityY = 0.0
                    }

                    "AAC3.1.2" -> {
                        event.y = 0.1699
                        velocityY = 0.0
                    }
                }
            }
        }
    }

    val onBlockBB = handler<BlockBBEvent> { event ->
        if (mc.player != null && (event.block is LadderBlock || event.block is VineBlock) &&
            mode == "AAC3.0.5" && mc.player.isClimbing
        )
            event.shape = null
    }

    override val tag
        get() = mode
}