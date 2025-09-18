/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category

object ChatControl : Module("ChatControl", Category.CLIENT, gameDetecting = false, subjective = true) {

    init {
        state = true
    }

    val chatLimitValue by boolean("NoChatLimit", true)
    val chatClearValue by boolean("NoChatClear", true)
    private val font by boolean("Font", false)

    fun shouldModifyChatFont() = handleEvents() && font
}