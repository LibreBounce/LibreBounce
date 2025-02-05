package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

internal object NoFallCancel : Choice("Cancel") {
    private val fallDistance = choices("FallDistance", Smart, arrayOf(Smart, Constant))
    private val resetFallDistance by boolean("ResetFallDistance", true)
    // not sure who would use this, but it's there I guess :|
    private val cancelSetback by boolean("CancelSetbackPacket", false)
    private var isFalling = false

    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    val packetHandler = handler<PacketEvent> {
        if (it.isCancelled) {
            return@handler
        }

        val packet = it.packet

        if (packet is PlayerPositionLookS2CPacket && isFalling) {
            val change = packet.change
            val pos = change.position()
            if (cancelSetback) {
                it.cancelEvent()
            }
            sendPacketSilently(
                PlayerMoveC2SPacket.Full(
                    pos.x, pos.y,
                    pos.z, change.yaw,
                    change.pitch, true, player.horizontalCollision
                )
            )
            isFalling = false
        } else if (packet is PlayerMoveC2SPacket && player.fallDistance >= fallDistance.activeChoice.value) {
            isFalling = true
            it.cancelEvent()
            if (resetFallDistance) {
                player.onLanding()
            }
        }
    }

    private abstract class DistanceMode(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
            get() = fallDistance

        abstract val value: Float
    }

    private object Smart : DistanceMode("Smart") {
        override val value: Float
            get() = player.getAttributeValue(EntityAttributes.SAFE_FALL_DISTANCE).toFloat()
    }

    private object Constant : DistanceMode("Constant") {
        override val value by float("Value", 1.7f, 0f..5f)
    }
}
