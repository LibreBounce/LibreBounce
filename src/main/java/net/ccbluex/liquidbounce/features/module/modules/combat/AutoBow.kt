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
import net.minecraft.item.ItemBow
import net.minecraft.network.packet.c2s.play.PlayerHandActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerHandActionC2SPacket.Action.RELEASE_USE_ITEM
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

object AutoBow : Module("AutoBow", Category.COMBAT, subjective = true) {

    private val waitForBowAimbot by boolean("WaitForBowAimbot", true)

    val onUpdate = handler<UpdateEvent> {
        mc.player?.run {
            if (isUsingItem && heldItem?.item is ItemBow && itemInUseDuration > 20
                && (!waitForBowAimbot || !ProjectileAimbot.handleEvents() || ProjectileAimbot.hasTarget())
            ) {
                stopUsingItem()
                sendPacket(PlayerHandActionC2SPacket(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
            }
        }
    }
}
