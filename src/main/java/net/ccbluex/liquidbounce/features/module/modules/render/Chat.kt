/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger

object Chat : Module("Chat", Category.RENDER, gameDetecting = false, subjective = true) {

    init {
        state = true
    }

    val roundedRadius by float("RoundedRadius", 0f, 0f..5f)
    val noMessageLimitValue by boolean("NoMessageLimit", true)
    val backgroundColor =
        ColorSettingsInteger(this, "BackgroundColor").with(a = 128)
    private val font by boolean("Font", false)

    fun shouldModifyChatFont() = handleEvents() && font
}