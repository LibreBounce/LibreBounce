/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.spartan

import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.ccbluex.liquidbounce.utils.timing.TickDelayTimer
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Position

object Spartan : FlyMode("Spartan") {
    private val timer = TickDelayTimer(12)

    override fun onEnable() {
        timer.reset()
    }

    override fun onUpdate() {
        mc.thePlayer?.apply {
            motionY = 0.0

            if (timer.resetIfPassed()) {
                sendPackets(
                    Position(posX, posY + 8, posZ, true),
                    Position(posX, posY - 8, posZ, true)
                )
            }
        }
    }
}
