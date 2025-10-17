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
import net.ccbluex.liquidbounce.features.module.modules.exploit.Ghost
import net.minecraft.client.gui.GuiGameOver

object AutoRespawn : Module("AutoRespawn", Category.PLAYER, gameDetecting = false) {

    private val instant by boolean("Instant", true)

    val onUpdate = handler<UpdateEvent> {
        mc.thePlayer?.run {
            if (Ghost.handleEvents())
                return@handler

            if (if (instant) health == 0F || isDead else mc.currentScreen is GuiGameOver && (mc.currentScreen as GuiGameOver).enableButtonsTimer >= 20) {
                respawnPlayer()
                mc.displayGuiScreen(null)
            }
        }
    }
}