/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vanilla

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.handleVanillaKickBypass
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.vanillaSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.keepAlive
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.network.play.client.C00PacketKeepAlive

object Vanilla : FlyMode("Vanilla") {

    override fun onMove(event: MoveEvent) {
        mc.thePlayer?.run {
            if (keepAlive) sendPacket(C00PacketKeepAlive())

            strafe(vanillaSpeed, true, event)

            onGround = false
            isInWeb = false

            capabilities.isFlying = false

            val ySpeed = when {
                mc.gameSettings.keyBindJump.isKeyDown -> vanillaSpeed
                mc.gameSettings.keyBindSneak.isKeyDown -> -vanillaSpeed
                else -> 0.0
            }

            motionY = ySpeed
            event.y = ySpeed

            handleVanillaKickBypass()
        }
    }
}
