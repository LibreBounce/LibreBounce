/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.serverOnGround
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

object Zoot : Module("Zoot", Category.PLAYER) {

    private val badEffects by boolean("BadEffects", true)
    private val fire by boolean("Fire", true)
    private val noAir by boolean("NoAir", false)

    val onUpdate = handler<UpdateEvent> {
        val player = mc.player ?: return@handler

        if (noAir && !serverOnGround)
            return@handler

        if (badEffects) {
            val effect = player.activePotionEffects
                .filter { it.potionID in NEGATIVE_EFFECT_IDS }
                .maxByOrNull { it.duration }

            if (effect != null) {
                repeat(effect.duration / 20) {
                    sendPacket(PlayerMoveC2SPacket(serverOnGround))
                }
            }
        }


        if (fire && mc.playerController.gameIsSurvivalOrAdventure() && player.isBurning) {
            repeat(9) {
                sendPacket(PlayerMoveC2SPacket(serverOnGround))
            }
        }
    }
}