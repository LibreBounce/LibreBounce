/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.ncp

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Step.isStep
import net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.StepMode
import net.minecraft.network.play.client.C03PacketPlayer

object OldNCP : StepMode("OldNCP") {

    override fun onPacket(event: PacketEvent) {
        if (packet is C03PacketPlayer && isStep) {
            packet.y += 0.07
            isStep = false
        }
    }
}