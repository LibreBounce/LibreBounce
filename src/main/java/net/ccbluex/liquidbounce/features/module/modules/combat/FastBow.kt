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
import net.ccbluex.liquidbounce.utils.extensions.rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.minecraft.item.ItemBow
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Angles
import net.minecraft.network.packet.c2s.play.PlayerHandActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerHandActionC2SPacket.Action.RELEASE_USE_ITEM
import net.minecraft.network.packet.c2s.play.PlayerUseC2SPacket
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

object FastBow : Module("FastBow", Category.COMBAT) {

    private val packets by int("Packets", 20, 3..20)

    val onUpdate = handler<UpdateEvent> {
        mc.thePlayer?.run {
            if (!isUsingItem)
                return@handler

            val currentItem = inventory.getCurrentItem()

            if (currentItem != null && currentItem.item is ItemBow) {
                sendPacket(
                    PlayerUseC2SPacket(
                        BlockPos.ORIGIN,
                        255,
                        currentEquippedItem,
                        0F,
                        0F,
                        0F
                    )
                )

                val (yaw, pitch) = currentRotation ?: rotation

                repeat(packets) {
                    sendPacket(Angles(yaw, pitch, true))
                }

                sendPacket(PlayerHandActionC2SPacket(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                itemInUseCount = currentItem.maxItemUseDuration - 1
            }
        }
    }
}