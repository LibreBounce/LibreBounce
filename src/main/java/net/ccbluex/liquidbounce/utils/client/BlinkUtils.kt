package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.network.Packet
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket
import net.minecraft.network.packet.s2c.play.SoundEventS2CPacket
import net.minecraft.network.packet.c2s.query.ServerStatusC2SPacket
import net.minecraft.network.packet.c2s.query.PingC2SPacket
import net.minecraft.util.math.Vec3d

object BlinkUtils : MinecraftInstance, Listenable {

    val publicPacket: Packet<*>? = null
    val packets = mutableListOf<Packet<*>>()
    val packetsReceived = mutableListOf<Packet<*>>()
    private var fakePlayer: EntityOtherPlayerMP? = null
    val positions = mutableListOf<Vec3d>()
    val isBlinking
        get() = (packets.size + packetsReceived.size) > 0

    // TODO: Make better & more reliable BlinkUtils.
    fun blink(packet: Packet<*>, event: PacketEvent, sent: Boolean? = true, receive: Boolean? = true) {
        val player = mc.player ?: return

        if (event.isCancelled || player.isDead || mc.currentServerData == null) return

        when (packet) {
            is HandshakeC2SPacket, is ServerStatusC2SPacket, is PingC2SPacket, is ChatMessageS2CPacket, is ChatMessageC2SPacket -> {
                return
            }

            is SoundEventS2CPacket -> {
                if (packet.soundName == "game.player.hurt") {
                    return
                }
            }
        }

        if (sent == true && receive == false) {
            if (event.eventType == EventState.RECEIVE) {
                synchronized(packetsReceived) {
                    PacketUtils.schedulePacketProcess(packetsReceived)
                }
                packetsReceived.clear()
            }
            if (event.eventType == EventState.SEND) {
                event.cancelEvent()
                synchronized(packets) {
                    packets += packet
                }
                if (packet is PlayerMoveC2SPacket && packet.isMoving) {
                    val packetPos = Vec3d(packet.x, packet.y, packet.z)
                    synchronized(positions) {
                        positions += packetPos
                    }
                }
            }
        }

        if (receive == true && sent == false) {
            if (event.eventType == EventState.RECEIVE && player.ticksExisted > 10) {
                event.cancelEvent()
                synchronized(packetsReceived) {
                    packetsReceived += packet
                }
            }
            if (event.eventType == EventState.SEND) {
                synchronized(packets) {
                    sendPackets(*packets.toTypedArray(), triggerEvents = false)
                }
                if (packet is PlayerMoveC2SPacket && packet.isMoving) {
                    val packetPos = Vec3d(packet.x, packet.y, packet.z)
                    synchronized(positions) {
                        positions += packetPos
                    }
                }
                packets.clear()
            }
        }

        if (sent == true && receive == true) {
            if (event.eventType == EventState.RECEIVE && player.ticksExisted > 10) {
                event.cancelEvent()
                synchronized(packetsReceived) {
                    packetsReceived += packet
                }
            }
            if (event.eventType == EventState.SEND) {
                event.cancelEvent()
                synchronized(packets) {
                    packets += packet
                }
                if (packet is PlayerMoveC2SPacket && packet.isMoving) {
                    val packetPos = Vec3d(packet.x, packet.y, packet.z)
                    synchronized(positions) {
                        positions += packetPos
                    }
                }
            }
        }

        if (sent == false && receive == false)
            unblink()
    }

    val onWorld = handler<WorldEvent> { event ->
        // Clear packets on disconnect only
        if (event.worldClient == null) {
            clear()
        }
    }

    fun syncSent() {
        synchronized(packetsReceived) {
            PacketUtils.schedulePacketProcess(packetsReceived)
            packetsReceived.clear()
        }
    }

    fun syncReceived() {
        synchronized(packets) {
            sendPackets(*packets.toTypedArray(), triggerEvents = false)
            packets.clear()
        }
    }

    fun cancel() {
        val player = mc.player ?: return
        val firstPosition = positions.firstOrNull() ?: return

        player.teleport(firstPosition.xCoord, firstPosition.yCoord, firstPosition.zCoord)

        synchronized(packets) {
            val iterator = packets.iterator()
            while (iterator.hasNext()) {
                val packet = iterator.next()
                if (packet is PlayerMoveC2SPacket) {
                    iterator.remove()
                } else {
                    sendPacket(packet)
                    iterator.remove()
                }
            }
        }

        synchronized(positions) {
            positions.clear()
        }

        // Remove fake player
        fakePlayer?.apply {
            fakePlayer?.entityId?.let { mc.world?.removeEntityFromWorld(it) }
            fakePlayer = null
        }
    }

    fun unblink() {
        synchronized(packetsReceived) {
            PacketUtils.schedulePacketProcess(packetsReceived)
        }
        synchronized(packets) {
            sendPackets(*packets.toTypedArray(), triggerEvents = false)
        }

        clear()

        // Remove fake player
        fakePlayer?.apply {
            fakePlayer?.entityId?.let { mc.world?.removeEntityFromWorld(it) }
            fakePlayer = null
        }
    }

    fun clear() {
        synchronized(packetsReceived) {
            packetsReceived.clear()
        }

        synchronized(packets) {
            packets.clear()
        }

        synchronized(positions) {
            positions.clear()
        }
    }

    fun addFakePlayer() {
        val player = mc.player ?: return
        val world = mc.world ?: return

        val faker = EntityOtherPlayerMP(world, player.gameProfile).apply {
            copyLocationAndAnglesFrom(player)
            rotationYaw = player.rotationYaw
            rotationPitch = player.rotationPitch
            headYaw = player.headYaw
            bodyYaw = player.bodyYaw
            inventory = player.inventory
        }

        world.addEntityToWorld(RandomUtils.nextInt(Int.MIN_VALUE, Int.MAX_VALUE), faker)

        fakePlayer = faker

        // Add positions indicating a blink start
        // val pos = thePlayer.positionVector
        // positions += pos.addVector(.0, thePlayer.eyeHeight / 2.0, .0)
        // positions += pos
    }
}