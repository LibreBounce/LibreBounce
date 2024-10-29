/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.value.BoolValue

object SilentHotbarModule : Module("SilentHotbar", Category.RENDER) {
    val keepHighlightedName by BoolValue("KeepHighlightedName", false)
    val keepHotbarSlot by BoolValue("KeepHotbarSlot", false)
    val keepItemInHandInFirstPerson by BoolValue("KeepItemInHandInFirstPerson", false)
    val keepItemInHandInThirdPerson by BoolValue("KeepItemInHandInThirdPerson", false)
}