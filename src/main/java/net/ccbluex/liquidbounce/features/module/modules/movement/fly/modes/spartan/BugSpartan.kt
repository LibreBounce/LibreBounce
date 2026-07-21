/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.spartan

import net.ccbluex.liquidbounce.features.module.modules.movement.fly.Fly.vanillaSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Position

object BugSpartan : FlyMode("BugSpartan") {
    override fun onEnable() {
        mc.player?.run {
            repeat(65) {
                sendPackets(
                    Position(posX, posY + 0.049, posZ, false),
                    Position(posX, posY, posZ, false)
                )
            }

            sendPacket(Position(posX, posY + 0.1, posZ, true))

            motionX *= 0.1
            motionZ *= 0.1
            swingItem()
        }
    }

    override fun onUpdate() {
        mc.player.abilities.flying = false

        mc.player.motionY = when {
            mc.gameOptions.jumpKey.isKeyDown -> vanillaSpeed.toDouble()
            mc.gameOptions.sneakKey.isKeyDown -> -vanillaSpeed.toDouble()
            else -> 0.0
        }

        strafe(vanillaSpeed, true)
    }
}
