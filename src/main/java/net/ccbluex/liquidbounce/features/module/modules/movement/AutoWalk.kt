/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.options.GameOptions

object AutoWalk : Module("AutoWalk", Category.MOVEMENT, subjective = true, gameDetecting = false) {
    val onUpdate = handler<UpdateEvent> {
        mc.gameOptions.forwardKey.pressed = true
    }

    override fun onDisable() {
        mc.gameOptions.forwardKey.pressed = GameOptions.isKeyDown(mc.gameOptions.forwardKey)
    }
}
