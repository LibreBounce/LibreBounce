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
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.QueuePacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallSpoofGround.DistanceMode.Constant
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallSpoofGround.DistanceMode.Smart
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager.Action
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

/**
 * SpoofGround mode for the NoFall module.
 * This mode spoofs the 'onGround' flag in PlayerMoveC2SPacket to prevent fall damage.
 */
internal object NoFallSpoofGround : Choice("SpoofGround") {
    private val fallDistance = choices("FallDistance", Smart, arrayOf(Smart, Constant))
    private val resetFallDistance by boolean("ResetFallDistance", true)
    private object Blink : ToggleableConfigurable(this, "Blink", false) {
        val disableOnSpoof by boolean("DisableOnSpoof", false)
        val predictionTicks by int("PredictionTicks", 10, 1..30)
    }

    init { tree(Blink) }

    private var lastFallDistance = 0f
    private var spoofFallDistance = 0f

    // Specify the parent configuration for this mode
    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    // Packet handler to intercept and modify PlayerMoveC2SPacket
    val packetHandler = handler<PacketEvent> {
        // Retrieve the packet from the event
        val packet = it.packet

        // Check if the packet is a PlayerMoveC2SPacket
        if (packet is PlayerMoveC2SPacket) {
            if (lastFallDistance > 0 && player.isOnGround) {
                spoofFallDistance = 0f
                if (Blink.enabled && !ModuleBlink.running) {
                    PacketQueueManager.flush { snapshot -> snapshot.origin == TransferOrigin.SEND }
                }
            }

            val distance = player.fallDistance - (if (resetFallDistance) spoofFallDistance else 0f)
            if (distance >= fallDistance.activeChoice.value) {
                // Modify the 'onGround' flag to true, preventing fall damage
                packet.onGround = true

                if (resetFallDistance) {
                    spoofFallDistance = player.fallDistance
                }

                if (Blink.enabled && Blink.disableOnSpoof && !ModuleBlink.running) {
                    PacketQueueManager.flush { snapshot -> snapshot.origin == TransferOrigin.SEND }
                }
            }

            lastFallDistance = player.fallDistance
        }
    }

    @Suppress("unused")
    private val blinkHandler = handler<QueuePacketEvent> { event ->
        if (event.origin != TransferOrigin.SEND || !Blink.enabled || player.isOnGround) {
            return@handler
        }

        if (player.fallDistance >= fallDistance.activeChoice.value) {
            event.action = Action.QUEUE
            return@handler
        }

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayer.SimulatedPlayerInput(
                DirectionalInput(player.input),
                player.input.playerInput.jump,
                player.isSprinting,
                true
            ))

        for (i in 1..Blink.predictionTicks) {
            simulatedPlayer.tick()
            if (simulatedPlayer.fallDistance >= fallDistance.activeChoice.value) {
                event.action = Action.QUEUE
                return@handler
            }
        }
    }

    private abstract class DistanceMode(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
            get() = fallDistance

        abstract val value: Float

        object Smart : DistanceMode("Smart") {
            override val value
                get() = player.getAttributeValue(EntityAttributes.SAFE_FALL_DISTANCE).toFloat()
        }

        object Constant : DistanceMode("Constant") {
            override val value by float("Value", 1.7f, 0f..5f)
        }
    }
}
