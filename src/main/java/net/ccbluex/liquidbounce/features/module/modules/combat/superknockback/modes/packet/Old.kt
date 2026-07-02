/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.modes.packet

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.modes.SuperKnockbackMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.*

object Old : SuperKnockbackMode("Old") {
    override fun onAttack(event: AttackEvent) {
        mc.thePlayer?.run {
            // Users reported that this mode is better than the other ones
            if (isSprinting)
                sendPacket(C0BPacketEntityAction(this, STOP_SPRINTING))

            sendPackets(
                C0BPacketEntityAction(this, START_SPRINTING),
                C0BPacketEntityAction(this, STOP_SPRINTING),
                C0BPacketEntityAction(this, START_SPRINTING)
            )

            isSprinting = true
            serverSprintState = true
        }
    }
}
