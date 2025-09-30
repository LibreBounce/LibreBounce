/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.spartan

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object Spartan : FlyMode("Spartan") {
    private val timer = TickTimer()

    override fun onEnable() {
        timer.reset()
    }

    override fun onUpdate() {
        val (x, y, z) = mc.thePlayer

        mc.thePlayer.motionY = 0.0

        timer.update()
        if (timer.hasTimePassed(12)) {
            sendPackets(
                C04PacketPlayerPosition(x, y + 8, z, true),
                C04PacketPlayerPosition(x, y - 8, z, true)
            )
            timer.reset()
        }
    }
}
