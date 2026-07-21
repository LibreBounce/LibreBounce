/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.vanilla

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.Fly.handleVanillaKickBypass
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.Fly.vanillaSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.Fly.keepAlive
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket

object Vanilla : FlyMode("Vanilla") {

    override fun onMove(event: MoveEvent) {
        mc.player?.run {
            if (keepAlive) sendPacket(KeepAliveC2SPacket())

            strafe(vanillaSpeed, true, event)

            onGround = false
            inCobweb = false

            abilities.flying = false

            val ySpeed = when {
                mc.gameOptions.jumpKey.isKeyDown -> vanillaSpeed.toDouble()
                mc.gameOptions.sneakKey.isKeyDown -> -vanillaSpeed.toDouble()
                else -> 0.0
            }

            motionY = ySpeed
            event.y = ySpeed

            handleVanillaKickBypass()
        }
    }
}
