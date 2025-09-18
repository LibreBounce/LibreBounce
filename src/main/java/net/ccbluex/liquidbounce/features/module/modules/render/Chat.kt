/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category

object Chat : Module("Chat", Category.RENDER, gameDetecting = false, subjective = true) {

    init {
        state = true
    }

    val noMessageLimitValue by boolean("NoMessageLimit", true)
    val font by font("Font", Fonts.fontSemibold40)
}