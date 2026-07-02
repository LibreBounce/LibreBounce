/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.modes.packet

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.SuperKnockback.ticks
import net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.modes.SuperKnockbackMode
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.*

object Silent : SuperKnockbackMode("Silent") {
    override fun onAttack(event: AttackEvent) {
        val player = mc.thePlayer

        if (player.isSprinting && player.serverSprintState) ticks = 2
    }

    override fun onPacket(event: PacketEvent) {
        val player = mc.thePlayer ?: return

        if (event.packet is C03PacketPlayer) {
            if (ticks == 2) {
                sendPacket(C0BPacketEntityAction(player, STOP_SPRINTING))
                ticks--
            } else if (ticks == 1 && player.isSprinting) {
                sendPacket(C0BPacketEntityAction(player, START_SPRINTING))
                ticks--
            }
        }
    }
}
