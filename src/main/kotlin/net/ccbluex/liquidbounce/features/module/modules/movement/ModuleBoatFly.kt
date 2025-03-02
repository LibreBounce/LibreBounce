package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.util.math.Vec3d
import net.minecraft.entity.player.PlayerEntity

/**
 * BoatFly module
 *
 * Allows the player to fly on a boat as if it was on water, even in the air or on land.
 */
object ModuleBoatFly : ClientModule("BoatFly", Category.MOVEMENT) {

    private val client: MinecraftClient = MinecraftClient.getInstance()

    // Adjustable speed parameter
    private val forwardBackSpeed by float("ForwardBackSpeed", 1.0f, 0.1f..10.0f)

    // Mode options
    private val mode by enumChoice("Mode", Mode.NORMAL).apply { tagBy(this) }
    private enum class Mode(override val choiceName: String) : NamedChoice {
        NORMAL("Normal"),
        INWATER("InWater"),
        MOTFLY("MotFly")
    }

    // Base speed
    private const val BASE_SPEED: Double = 0.1
    // Acceleration factor (for smooth speed increase)
    private const val ACCELERATION_FACTOR: Double = 0.2
    // Vertical speed control (up and down movement)
    private const val VERTICAL_SPEED: Double = 0.1

    @Suppress("unused")
    private val boatFlyHandler = handler<GameTickEvent> {
        val player = client.player ?: return@handler
        val world = client.world ?: return@handler

        if (player.vehicle is BoatEntity) {
            val boat = player.vehicle as BoatEntity

         
            val moveInput = client.options.forwardKey.isPressed ||
                client.options.backKey.isPressed ||
                client.options.leftKey.isPressed ||
                client.options.rightKey.isPressed

          
            val targetSpeed = if (moveInput) forwardBackSpeed.toDouble() * BASE_SPEED else 0.0

           
            val currentVelX = boat.velocity.x
            val currentVelZ = boat.velocity.z
            val currentSpeed = Math.sqrt(currentVelX * currentVelX + currentVelZ * currentVelZ)

          
            var newSpeed = when {
                targetSpeed == 0.0 -> 0.0
                currentSpeed == 0.0 -> targetSpeed
                currentSpeed < targetSpeed -> currentSpeed + (targetSpeed - currentSpeed) * ACCELERATION_FACTOR
                else -> targetSpeed
            }

            val (newVelX, newVelZ) = if (currentSpeed != 0.0) {
                val normX = currentVelX / currentSpeed
                val normZ = currentVelZ / currentSpeed
                Pair(normX * newSpeed, normZ * newSpeed)
            } else {
                Pair(0.0, 0.0)
            }

            
            var newVelY = boat.velocity.y

            if (player.isOnGround) {
              
                newVelY = boat.velocity.y
            } else if (player.isSneaking) {
         
                newVelY = -VERTICAL_SPEED
            } else if (client.options.jumpKey.isPressed) {
      
                newVelY = VERTICAL_SPEED
            }

            when (mode) {
                Mode.NORMAL -> {
                    boat.setVelocity(newVelX, newVelY, newVelZ)

                    val blockBelow = world.getBlockState(boat.blockPos.down())

                    if (!blockBelow.isOf(Blocks.WATER) && !blockBelow.isAir) {
                        if (player.isSneaking) {
                            newVelY = -VERTICAL_SPEED
                        } else if (client.options.jumpKey.isPressed) {
                            newVelY = VERTICAL_SPEED
                        } else {
                            newVelY = boat.velocity.y
                        }

                        boat.setVelocity(newVelX, newVelY, newVelZ)
                    } else {
                        boat.setVelocity(newVelX, boat.velocity.y, newVelZ)
                    }
                }


                Mode.INWATER -> {
                    boat.setVelocity(newVelX, newVelY, newVelZ)
                    val blockBelow = world.getBlockState(boat.blockPos.down())
                    if (!blockBelow.isOf(Blocks.WATER)) {
                        boat.setVelocity(newVelX, 0.0, newVelZ)
                    }
                    val world = boat.world
                    val pos = boat.blockPos.down()
                    world.setBlockState(pos, Blocks.WATER.defaultState, 3)
                }

                Mode.MOTFLY -> {
                    boat.setVelocity(newVelX, newVelY, newVelZ)
                    val isSinglePlayer = MinecraftClient.getInstance().isInSingleplayer
                    val velocity = Vec3d(newVelX, newVelY, newVelZ)

                    if (isSinglePlayer) {
                        boat.updatePosition(boat.x + newVelX, boat.y + newVelY, boat.z + newVelZ)
                    } else {
                        val entityMovePacket = EntityVelocityUpdateS2CPacket(boat.id, velocity)
                        client.networkHandler?.sendPacket(entityMovePacket)
                    }

                    val blockBelow = world.getBlockState(boat.blockPos.down())
                    if (!blockBelow.isOf(Blocks.WATER) && !blockBelow.isAir) {
                        boat.setVelocity(newVelX, 0.0, newVelZ)  
                    } else {
                        boat.setVelocity(newVelX, boat.velocity.y, newVelZ)
                    }
                }


            }
        }
    }

   fun BoatEntity.isTouchingWater(): Boolean {
        val world = this.world
        val blockBelow = world.getBlockState(this.blockPos.down())
        return blockBelow.isOf(Blocks.WATER)
    }
}
