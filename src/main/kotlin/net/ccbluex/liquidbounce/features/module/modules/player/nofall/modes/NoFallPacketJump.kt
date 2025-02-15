package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.QueuePacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallPacketJump.DistanceMode.Constant
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallPacketJump.DistanceMode.Smart
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallPacketJump.Timing.Falling
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallPacketJump.Timing.Landing
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager.Action
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

internal object NoFallPacketJump : Choice("PacketJump") {
    private val packetType by enumChoice("PacketType", MovePacketType.FULL,
        arrayOf(MovePacketType.FULL, MovePacketType.POSITION_AND_ON_GROUND))
    private val fallDistance = choices("FallDistance", Smart, arrayOf(Smart, Constant))
    private val timing = choices("Timing", Landing, arrayOf(Landing, Falling))

    private var falling = false

    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    val tickHandler = handler<PlayerTickEvent> {
        val resetFallDistance = timing.activeChoice is Falling && Falling.resetFallDistance
        val distance = player.fallDistance - (if (resetFallDistance) Falling.packetFallDistance else 0f)
        falling = distance >= fallDistance.activeChoice.value
        if (timing.activeChoice is Falling && !player.isOnGround && falling) {
            network.sendPacket(packetType.generatePacket().apply {
                y += 1.0E-9
            })

            Falling.onPacket()
        }
    }

    val packetHandler = handler<PacketEvent> { event ->
        if (timing.activeChoice is Landing && event.packet is PlayerMoveC2SPacket && event.packet.onGround && falling) {
            falling = false
            network.sendPacket(packetType.generatePacket().apply {
                x = player.lastX
                y = player.lastBaseY + 1.0E-9
                z = player.lastZ
                onGround = false
            })
        }
    }

    private abstract class Timing(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
            get() = timing

        object Landing : Timing("Landing")

        object Falling : Timing("Falling") {
            val resetFallDistance by boolean("ResetFallDistance", true)
            object Blink : ToggleableConfigurable(this, "Blink", false) {
                val disableOnSpoof by boolean("DisableOnSpoof", false)
                private val predictionTicks by int("PredictionTicks", 10, 1..30)

                @Suppress("unused")
                private val blinkHandler = handler<QueuePacketEvent> { event ->
                    if (event.origin != TransferOrigin.SEND || player.isOnGround || player.age <= 20) {
                        return@handler
                    }

                    if (player.fallDistance >= fallDistance.activeChoice.value && player.age > 20) {
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
                        if (simulatedPlayer.fallDistance >= fallDistance.activeChoice.value) {
                            event.action = Action.QUEUE
                            return@handler
                        }
                    }
                }
            }

            init { tree(Blink) }

            private var lastFallDistance = 0f
            var packetFallDistance = 0f

            fun onPacket() {
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
            override val value by float("Value", 3f, 0f..5f)
        }
    }
}
