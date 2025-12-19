/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.block.BlockSlab
import net.minecraft.block.BlockSlime
import net.minecraft.block.BlockStairs
import net.minecraft.init.Blocks.*
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos

object BufferSpeed : Module("BufferSpeed", Category.MOVEMENT) {

    private val speedLimit by boolean("SpeedLimit", true)
    private val maxSpeed by float("MaxSpeed", 2f, 1f..5f) { speedLimit }

    private val buffer by boolean("Buffer", true)

    private val stairsMode by choices("Stairs", arrayOf("Off", "Old", "New"), "New")
    private val stairsBoost by float("StairsBoost", 1.87f, 1f..2f) { stairsMode == "Old" }

    private val slabsMode by choices("SlabsMode", arrayOf("Off", "Old", "New"), "New")
    private val slabsBoost by float("SlabsBoost", 1.87f, 1f..2f) { slabsMode == "Old" }

    private val doIce by boolean("Ice", false)
    private val iceBoost by float("IceBoost", 1.342f, 1f..2f) { doIce }

    private val snow by boolean("Snow", true)
    private val snowBoost by float("SnowBoost", 1.87f, 1f..2f) { snow }
    private val snowPort by boolean("SnowPort", true) { snow }

    private val wallMode by choices("WallMode", arrayOf("Off", "Old", "New"), "New")
    private val wallBoost by float("WallBoost", 1.87f, 1f..2f) { wallMode == "Old" }

    private val headBlock by boolean("HeadBlock", true)
    private val headBlockBoost by float("HeadBlockBoost", 1.87f, 1f..2f) { headBlock }

    private val slime by boolean("Slime", true)
    private val airStrafe by boolean("AirStrafe", false)
    private val noHurt by boolean("NoHurt", true)

    private var speed = 0.0
    private var down = false
    private var forceDown = false
    private var fastHop = false
    private var hadFastHop = false
    private var legitHop = false

    val onUpdate = handler<UpdateEvent> {
        mc.thePlayer?.run {
            if (Speed.handleEvents() || noHurt && hurtTime > 0) {
                reset()
                return@handler
            }

            val blockPos = BlockPos(this)

            if (forceDown || down && motionY == 0.0) {
                motionY = -1.0
                down = false
                forceDown = false
            }

            if (fastHop) {
                speedInAir = 0.0211f
                hadFastHop = true
            } else if (hadFastHop) {
                speedInAir = 0.02f
                hadFastHop = false
            }

            if (!isMoving || isSneaking || isInWater || mc.gameSettings.keyBindJump.isKeyDown) {
                reset()
                return@handler
            }

            if (onGround) {
                fastHop = false

                if (slime && (blockPos.down().block is BlockSlime || blockPos.block is BlockSlime)) {
                    tryJump()

                    motionX = motionY * 1.132
                    motionY = 0.08
                    motionZ = motionY * 1.132

                    down = true
                    return@handler
                }

                if (blockPos.block is BlockSlab) {
                    when (slabsMode) {
                        "Old" -> {
                            boost(slabsBoost)
                            return@handler
                        }

                        "New" -> {
                            fastHop = true
                            if (legitHop) {
                                tryJump()
                                onGround = false
                                legitHop = false
                                return@handler
                            }
                            onGround = false

                            strafe(0.375f)

                            tryJump()
                            motionY = 0.41
                            return@handler
                        }
                    }
                }

                if (blockPos.down().block is BlockStairs || blockPos.block is BlockStairs) {
                    when (stairsMode) {
                        "Old" -> {
                            boost(stairsBoost)
                            return@handler
                        }

                        "New" -> {
                            fastHop = true

                            if (legitHop) {
                                tryJump()
                                onGround = false
                                legitHop = false
                                return@handler
                            }

                            onGround = false
                            strafe(0.375f)
                            tryJump()
                            motionY = 0.41
                            return@handler
                        }
                    }
                }

                legitHop = true

                if (headBlock && blockPos.up(2).block != air) {
                    boost(headBlockBoost)
                    return@handler
                }

                if (doIce && blockPos.down().block.let { it == ice || it == packed_ice }) {
                    boost(iceBoost)
                    return@handler
                }

                if (snow && blockPos.block == snow_layer && (snowPort || posY - posY.toInt() >= 0.12500)) {
                    if (posY - posY.toInt() >= 0.12500) {
                        boost(snowBoost)
                    } else {
                        tryJump()
                        forceDown = true
                    }
                    return@handler
                }

                when (wallMode) {
                    "Old" -> if (isCollidedVertically && isNearBlock || BlockPos(this).up(2).block != air) {
                        boost(wallBoost)
                        return@handler
                    }

                    "New" ->
                        if (isNearBlock && !movementInput.jump) {
                            tryJump()
                            motionY = 0.08
                            motionX *= 0.99
                            motionZ *= 0.99
                            down = true
                            return@handler
                        }
                    }
                }

                val currentSpeed = speed

                if (speed < currentSpeed) speed = currentSpeed

                if (buffer && speed > 0.2) {
                    speed /= 1.0199999809265137
                    strafe()
                }
            } else {
                speed = 0.0

                if (airStrafe) strafe()
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        if (event.packet is S08PacketPlayerPosLook)
            speed = 0.0
    }

    override fun onEnable() = reset()

    override fun onDisable() = reset()

    private fun reset() {
        val player = mc.thePlayer ?: return
        legitHop = true
        speed = 0.0

        if (hadFastHop) {
            player.speedInAir = 0.02f
            hadFastHop = false
        }
    }

    private fun boost(boost: Float) {
        mc.thePlayer.motionX *= boost
        mc.thePlayer.motionZ *= boost

        speed = MovementUtils.speed.toDouble()

        if (speedLimit && speed > maxSpeed)
            speed = maxSpeed.toDouble()
    }

    private val isNearBlock: Boolean
        get() {
            val player = mc.thePlayer
            val blocks = arrayOf(
                BlockPos(player.posX, player.posY + 1, player.posZ - 0.7),
                BlockPos(player.posX + 0.7, player.posY + 1, player.posZ),
                BlockPos(player.posX, player.posY + 1, player.posZ + 0.7),
                BlockPos(player.posX - 0.7, player.posY + 1, player.posZ)
            )

            for (blockPos in blocks) {
                val blockState = mc.theWorld.getBlockState(blockPos)

                val collisionBoundingBox = blockState.block.getCollisionBoundingBox(mc.theWorld, blockPos, blockState)

                if ((collisionBoundingBox == null || collisionBoundingBox.maxX ==
                            collisionBoundingBox.minY + 1) &&
                    !blockState.block.isTranslucent && blockState.block == water &&
                    blockState.block !is BlockSlab || blockState.block == barrier
                ) return true
            }
            return false
        }
}