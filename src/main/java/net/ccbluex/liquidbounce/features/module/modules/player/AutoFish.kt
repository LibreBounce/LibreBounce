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
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.item.ItemFishingRod

object AutoFish : Module("AutoFish", Category.PLAYER, subjective = true, gameDetecting = false) {

    private val rodOutTimer = MSTimer()

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer

        if (player?.heldItem == null || player.heldItem.item !is ItemFishingRod)
            return@handler

        val fishEntity = player.fishEntity

        if (rodOutTimer.hasTimePassed(500) && fishEntity == null || (fishEntity != null && fishEntity.motionX == 0.0 && fishEntity.motionZ == 0.0 && fishEntity.motionY != 0.0)) {
            mc.rightClickMouse()
            rodOutTimer.reset()
        }
    }
}
