/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.blocksmc

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.movement.copy
import net.ccbluex.liquidbounce.utils.movement.zeroXZ
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * extensive blocksmc speed
 * @author liquidsquid1
 */

class SpeedBlocksMC(override val parent: ChoiceConfigurable<*>) : Choice("BlocksMC") {

    private val fullStrafe by boolean("FullStrafe", true)
    private val lowHop by boolean("LowHop", true)
    private val damageBoost by boolean("DamageBoost", true)
    private val damageLowHop by boolean("DamageLowHop", false)
    private val safeY by boolean("SafeY", true)

    private var airTicks = 0
    private var canSpeed = false

    private var lastVelocity = 0

    override fun enable() {
        canSpeed = false
        lastVelocity = 9999
    }

    override fun disable() {
        airTicks = 0
        player.strafe(speed = 0.0)
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (player.isOnGround) {
            airTicks = 0
            canSpeed = true
        } else {
            airTicks++

            if (!canSpeed) {
                return@tickHandler
            }
            if (fullStrafe) {
                if (player.moving) {
                    player.strafe(speed = player.sqrtSpeed - 0.004)
                }
            } else {
                if (airTicks >= 6 && player.moving) {
                    player.strafe()
                }
            }

            if ((player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0) > 0 && airTicks == 3) {
                player.velocity = player.velocity.multiply(
                    1.12,
                    1.0,
                    1.12
                )
            }

            if (lowHop && airTicks == 4) {
                if (safeY) {
                    if (player.y % 1.0 == 0.16610926093821377) {
                        player.velocity.y = -0.09800000190734863
                    }
                } else {
                    player.velocity.y = -0.09800000190734863
                }
            }

            if (damageBoost && player.moving) {
                when (lastVelocity) {
                    1, 2 -> {
                        player.strafe(speed = 1.1)
                    }
                    3, 4, 5, 6, 7, 8, 9 -> {
                        player.strafe(speed = 1.0)
                    }
                    10, 11, 12, 13, 14 -> {
                        player.strafe(speed = 0.75)
                    }
                    15, 16, 17, 18, 19, 20 -> {
                        player.strafe(speed = 0.5)
                    }
                }
            }

            if (damageLowHop && player.hurtTime >= 1) {
                if (player.velocity.y > 0) {
                    player.velocity.y -= 0.15
                }
            }

        }

        lastVelocity++

    }

    @Suppress("unused")
    private val jumpHandler = handler<PlayerJumpEvent> {
        val atLeast = 0.281 + 0.2 * (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0)
        if (!canSpeed) {
            return@handler
        }

        if (!player.moving) {
            return@handler
        }

        player.strafe(speed = player.sqrtSpeed.coerceAtLeast(atLeast) - 0.01)
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent> { event ->
        if (event.directionalInput.isMoving) {
            event.input.copy(jump = true)
        }
    }

    @Suppress("unused")
    val packetHandler = sequenceHandler<PacketEvent>(priority = EventPriorityConvention.FIRST_PRIORITY) { event ->
        val packet = event.packet

        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id) {
            val velocityX = packet.velocityX / 8000.0
            val velocityY = packet.velocityY / 8000.0
            val velocityZ = packet.velocityZ / 8000.0

            waitTicks(1)

            // Fall damage velocity
            val fallDamage = velocityX == 0.0 && velocityZ == 0.0 && velocityY == -0.078375
            if (!fallDamage) {
                lastVelocity = 0
            }

            return@sequenceHandler
        }

        if (packet is PlayerPositionLookS2CPacket) {
            lastVelocity = 9999
            player.zeroXZ()
        }
    }

}
