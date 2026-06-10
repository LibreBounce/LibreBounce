/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.other

import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.timing.TickDelayTimer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object Spartan : NoFallMode("Spartan") {
    private val spartanTimer = TickDelayTimer(10)

    override fun onUpdate() {
        mc.thePlayer?.run {
            if (fallDistance > 1.5 && spartanTimer.resetIfPassed()) {
                sendPackets(
                    C04PacketPlayerPosition(posX, posY + 10, posZ, true),
                    C04PacketPlayerPosition(posX, posY - 10, posZ, true)
                )
            }
        }
    }
}