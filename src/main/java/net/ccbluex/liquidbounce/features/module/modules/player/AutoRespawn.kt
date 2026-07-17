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
import net.minecraft.client.gui.screen.DeathScreen

object AutoRespawn : Module("AutoRespawn", Category.PLAYER, gameDetecting = false) {

    private val instant by boolean("Instant", true)

    val onUpdate = handler<UpdateEvent> {
        mc.player?.run {
            if (Ghost.handleEvents())
                return@handler

            if (if (instant) health == 0F || isDead else mc.currentScreen is DeathScreen && (mc.currentScreen as DeathScreen).enableButtonsTimer >= 20) {
                respawnPlayer()
                mc.displayScreen(null)
            }
        }
    }
}