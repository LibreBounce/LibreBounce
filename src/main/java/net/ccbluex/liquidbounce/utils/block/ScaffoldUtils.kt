package net.ccbluex.liquidbounce.utils.block

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.timing.TickTimer

object ScaffoldUtils : MinecraftInstance, Listenable {
    var lastDamageTicks = TickTimer()

    val onPacket = handler<PacketEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        val packet = event.packet

        if (packet is S12PacketEntityVelocity || packet is S27PacketExplosion) {
            lastDamageTicks.reset()
        }
    }
}