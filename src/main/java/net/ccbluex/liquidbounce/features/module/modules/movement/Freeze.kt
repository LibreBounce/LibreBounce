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
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerMoveS2CPacket

object Freeze : Module("Freeze", Category.MOVEMENT) {
    private var motionX = 0.0
    private var motionY = 0.0
    private var motionZ = 0.0
    private var x = 0.0
    private var y = 0.0
    private var z = 0.0

    override fun onEnable() {
        val player = mc.player ?: return

        x = player.posX
        y = player.posY
        z = player.posZ
        motionX = player.motionX
        motionY = player.motionY
        motionZ = player.motionZ
    }

    val onUpdate = handler<UpdateEvent> {
        mc.player.motionX = 0.0
        mc.player.motionY = 0.0
        mc.player.motionZ = 0.0
        mc.player.updatePositionAndAngles(x, y, z, mc.player.rotationYaw, mc.player.rotationPitch)
    }

    val onPacket = handler<PacketEvent> { event ->
        if (event.packet is PlayerMoveC2SPacket) {
            event.cancelEvent()
        }
        if (event.packet is PlayerMoveS2CPacket) {
            x = event.packet.x
            y = event.packet.y
            z = event.packet.z
            motionX = 0.0
            motionY = 0.0
            motionZ = 0.0
        }
    }

    override fun onDisable() {
        mc.player.motionX = motionX
        mc.player.motionY = motionY
        mc.player.motionZ = motionZ
        mc.player.updatePositionAndAngles(x, y, z, mc.player.rotationYaw, mc.player.rotationPitch)
    }
}
