/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.QueuePacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager.Action
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.entity.attribute.EntityAttributes

internal object NoFallPacket : Choice("Packet") {
    private val packetType by enumChoice("PacketType", MovePacketType.FULL)
    private val filter = choices("Filter", Filter.FallDistance, arrayOf(Filter.FallDistance, Filter.Always))

    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    val repeatable = tickHandler {
        if (filter.activeChoice.isActive) {
            network.sendPacket(packetType.generatePacket().apply {
                onGround = true
            })

            filter.activeChoice.onPacket()
        }
    }

    private abstract class Filter(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
            get() = filter

        abstract val isActive: Boolean
        open fun onPacket() {}

        object FallDistance : Filter("FallDistance") {
            override val isActive
                get() = player.fallDistance - (if (resetFallDistance) packetFallDistance else 0f) - player.velocity.y >= distance.activeChoice.value && player.age > 20

            private val distance = choices("Distance", DistanceMode.Smart, arrayOf(DistanceMode.Smart, DistanceMode.Constant))
            val resetFallDistance by boolean("ResetFallDistance", true)
            object Blink : ToggleableConfigurable(this, "Blink", false) {
                val disableOnSpoof by boolean("DisableOnSpoof", false)
                private val predictionTicks by int("PredictionTicks", 10, 1..30)

                @Suppress("unused")
                private val blinkHandler = handler<QueuePacketEvent> { event ->
                    if (event.origin != TransferOrigin.SEND || player.isOnGround || player.age <= 20) {
                        return@handler
                    }

                    if (player.fallDistance >= distance.activeChoice.value && player.age > 20) {
                        event.action = Action.QUEUE
                        return@handler
                    }

                    val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
                        SimulatedPlayer.SimulatedPlayerInput(
                            DirectionalInput(player.input),
                            player.input.playerInput.jump,
                            player.isSprinting,
                            true
                        )
                    )

                    for (i in 0..predictionTicks) {
                        simulatedPlayer.tick()
                        if (simulatedPlayer.fallDistance >= distance.activeChoice.value) {
                            event.action = Action.QUEUE
                            return@handler
                        }
                    }
                }
            }

            init { tree(Blink) }

            private var lastFallDistance = 0f
            private var packetFallDistance = 0f

            override fun onPacket() {
                packetFallDistance = player.fallDistance

                if (Blink.running && Blink.disableOnSpoof && !ModuleBlink.running) {
                    PacketQueueManager.flush { snapshot -> snapshot.origin == TransferOrigin.SEND }
                }
            }

            val repeatable = tickHandler {
                if (lastFallDistance > 0 && player.isOnGround) {
                    packetFallDistance = 0f
                    if (Blink.running && !ModuleBlink.running) {
                        PacketQueueManager.flush { snapshot -> snapshot.origin == TransferOrigin.SEND }
                    }
                }

                lastFallDistance = player.fallDistance
            }

            private abstract class DistanceMode(name: String) : Choice(name) {
                override val parent: ChoiceConfigurable<*>
                    get() = distance

                abstract val value: Float

                object Smart : DistanceMode("Smart") {
                    override val value: Float
                        get() = player.getAttributeValue(EntityAttributes.SAFE_FALL_DISTANCE).toFloat()
                }

                object Constant : DistanceMode("Constant") {
                    override val value by float("Value", 2f, 0f..5f)
                }
            }
        }

        object Always : Filter("Always") {
            override val isActive
                get() = true
        }
    }
}
