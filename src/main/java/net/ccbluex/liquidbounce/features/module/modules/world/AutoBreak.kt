/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.block
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Blocks

object AutoBreak : Module("AutoBreak", Category.WORLD, subjective = true, gameDetecting = false) {

    val onUpdate = handler<UpdateEvent> {
        mc.theWorld ?: return@handler

        val target = mc.objectMouseOver?.blockPos ?: return@handler

        mc.gameSettings.keyBindAttack.pressed = target.block != Blocks.air
    }

    override fun onDisable() {
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindAttack))
            mc.gameSettings.keyBindAttack.pressed = false
    }
}
