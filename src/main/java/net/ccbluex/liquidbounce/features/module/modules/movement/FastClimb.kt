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
import net.minecraft.block.BlockLadder
import net.minecraft.block.BlockVine
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

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
        mc.thePlayer?.run {
            motionY = 0.0
            isInWeb = true
            onGround = true

            isInWeb = false
        }
    }

    val onMove = handler<MoveEvent> { event ->
        mc.thePlayer?.run {
            when {
                mode == "AAC3.0.0" && isCollidedHorizontally -> {
                    var x = 0.0
                    var z = 0.0

                    when (horizontalFacing) {
                        EnumFacing.NORTH -> z = -0.99
                        EnumFacing.EAST -> x = 0.99
                        EnumFacing.SOUTH -> z = 0.99
                        EnumFacing.WEST -> x = -0.99
                        else -> {}
                    }

                    val block = BlockPos(posX + x, posY, posZ + z).block

                    if (block is BlockLadder || block is BlockVine) {
                        event.y = 0.5
                        motionY = 0.0
                    }
                }

                mode == "AAC3.0.5" && mc.gameSettings.keyBindForward.isKeyDown &&
                    collideBlockIntersects(entityBoundingBox) {
                        it is BlockLadder || it is BlockVine
                    } -> {
                    event.x = 0.0
                    event.y = 0.5
                    event.z = 0.0

                    motionX = 0.0
                    motionY = 0.0
                    motionZ = 0.0
                }

                mode == "Clip" && isOnLadder && mc.gameSettings.keyBindForward.isKeyDown -> {
                    for (i in posY.toInt()..posY.toInt() + 8) {
                        val block = BlockPos(posX, i.toDouble(), posZ).block

                        if (block !is BlockLadder) {
                            var x = 0.0
                            var z = 0.0

                            when (horizontalFacing) {
                                EnumFacing.NORTH -> z = -1.0
                                EnumFacing.EAST -> x = 1.0
                                EnumFacing.SOUTH -> z = 1.0
                                EnumFacing.WEST -> x = -1.0
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

            if (isCollidedHorizontally && isOnLadder) {
                when (mode) {
                    "Vanilla" -> {
                        event.y = speed.toDouble()
                        motionY = 0.0
                    }

                    "Delay" -> {
                        if (climbCount >= climbDelay) {
                            event.y = climbSpeed.toDouble()
                            playerClimb()

                            sendPacket(C04PacketPlayerPosition(posX, posY, posZ, true))
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
        if (mc.thePlayer != null && (event.block is BlockLadder || event.block is BlockVine) &&
            mode == "AAC3.0.5" && mc.thePlayer.isOnLadder
        )
            event.boundingBox = null
    }

    override val tag
        get() = mode
}