/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.modes.packet

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.modes.SuperKnockbackMode
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.*

object SneakPacket : SuperKnockbackMode("SneakPacket") {
    override fun onAttack(event: AttackEvent) {
        val player = mc.thePlayer

        sendPackets(
            C0BPacketEntityAction(player, STOP_SPRINTING),
            C0BPacketEntityAction(player, START_SNEAKING),
            C0BPacketEntityAction(player, START_SPRINTING),
            C0BPacketEntityAction(player, STOP_SNEAKING)
        )
    }
}