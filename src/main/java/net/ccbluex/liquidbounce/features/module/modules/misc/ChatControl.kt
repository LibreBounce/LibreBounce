/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.value.boolean

object ChatControl : Module("ChatControl", Category.MISC, gameDetecting = false, hideModule = false) {

    val noChatClear by boolean("NoChatClear", true)
    // TODO: Add stackMessage
    val stackMessage by boolean("StackMessage", true)
    val noLengthLimit by boolean("NoLengthLimit", true)
    val forceUnicodeChat by boolean("ForceUnicodeChat", false)

}
