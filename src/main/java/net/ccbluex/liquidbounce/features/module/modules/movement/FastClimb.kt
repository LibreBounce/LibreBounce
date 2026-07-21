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
            motionY = 0.0
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

                    val block = BlockPos(posX + x, posY, posZ + z).block

                    if (block is LadderBlock || block is VineBlock) {
                        event.y = 0.5
                        motionY = 0.0
                    }
                }

                mode == "AAC3.0.5" && mc.gameOptions.forwardKey.isKeyDown &&
                    collideBlockIntersects(shape) {
                        it is LadderBlock || it is VineBlock
                    } -> {
                    event.x = 0.0
                    event.y = 0.5
                    event.z = 0.0

                    motionX = 0.0
                    motionY = 0.0
                    motionZ = 0.0
                }

                mode == "Clip" && isOnLadder && mc.gameOptions.forwardKey.isKeyDown -> {
                    for (i in posY.toInt()..posY.toInt() + 8) {
                        val block = BlockPos(posX, i.toDouble(), posZ).block

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

                            setPosition(posX + x, i.toDouble(), posZ + z)
                            break
                        } else {
                            setPosition(posX, i.toDouble(), posZ)
                        }
                    }
                }
            }

            if (collidingHorizontally && isOnLadder) {
                when (mode) {
                    "Vanilla" -> {
                        event.y = speed.toDouble()
                        motionY = 0.0
                    }

                    "Delay" -> {
                        if (climbCount >= climbDelay) {
                            event.y = climbSpeed.toDouble()
                            playerClimb()

                            sendPacket(Position(posX, posY, posZ, true))
                            climbCount = 0
                        } else {
                            posY = prevPosY

                            playerClimb()
                            climbCount++

                        }
                    }

                    "SAAC3.1.2" -> {
                        event.y = 0.1649
                        motionY = 0.0
                    }

                    "AAC3.1.2" -> {
                        event.y = 0.1699
                        motionY = 0.0
                    }
                }
            }
        }
    }

    val onBlockBB = handler<BlockBBEvent> { event ->
        if (mc.player != null && (event.block is LadderBlock || event.block is VineBlock) &&
            mode == "AAC3.0.5" && mc.player.isOnLadder
        )
            event.boundingBox = null
    }

    override val tag
        get() = mode
}