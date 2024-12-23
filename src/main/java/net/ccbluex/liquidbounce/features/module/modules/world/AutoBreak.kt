/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.loopHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.block
import net.minecraft.init.Blocks.air

object AutoBreak : Module("AutoBreak", Category.WORLD, subjective = true, gameDetecting = false) {

    val onUpdate = loopHandler {
        if (mc.objectMouseOver == null || mc.objectMouseOver.blockPos == null || mc.theWorld == null)
            return@loopHandler

        mc.gameSettings.keyBindAttack.pressed = mc.objectMouseOver.blockPos.block != air
    }

    override fun onDisable() {
        if (!mc.gameSettings.keyBindAttack.pressed)
            mc.gameSettings.keyBindAttack.pressed = false
    }
}
