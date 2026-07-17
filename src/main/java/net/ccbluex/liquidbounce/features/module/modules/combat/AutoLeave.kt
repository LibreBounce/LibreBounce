/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Action.ATTACK
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Position

object AutoLeave : Module("AutoLeave", Category.COMBAT, subjective = true) {
    private val health by float("Health", 8f, 0f..20f)
    private val mode by choices("Mode", arrayOf("Quit", "InvalidPacket", "SelfHurt", "IllegalChat"), "Quit")

    val onUpdate = handler<UpdateEvent> {
        val player = mc.player ?: return@handler

        if (player.health <= health && !player.capabilities.isCreativeMode && !mc.isIntegratedServerRunning) {
            when (mode) {
                "Quit" -> mc.world.sendQuittingDisconnectingPacket()
                "InvalidPacket" -> sendPacket(
                    Position(
                        Double.NaN,
                        Double.NEGATIVE_INFINITY,
                        Double.POSITIVE_INFINITY,
                        !player.onGround
                    )
                )
                "SelfHurt" -> sendPacket(PlayerInteractEntityC2SPacket(player, ATTACK))
                "IllegalChat" -> player.sendChatMessage(nextInt().toString() + "§§§" + nextInt())
            }

            state = false
        }
    }
}