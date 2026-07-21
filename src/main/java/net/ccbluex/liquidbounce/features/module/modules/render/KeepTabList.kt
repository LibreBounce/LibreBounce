/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.options.GameOptions

object KeepTabList : Module("KeepTabList", Category.RENDER, gameDetecting = false) {

    val onUpdate = handler<UpdateEvent> {
        if (mc.player == null || mc.world == null) return@handler

        mc.gameOptions.keyBindPlayerList.pressed = true
    }

    override fun onDisable() {
        mc.gameOptions.keyBindPlayerList.pressed = GameOptions.isKeyDown(mc.gameOptions.keyBindPlayerList)
    }
}