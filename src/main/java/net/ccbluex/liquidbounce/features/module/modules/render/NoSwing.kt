/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.base.Category
import net.ccbluex.liquidbounce.features.module.base.Module

object NoSwing : Module("NoSwing", Category.RENDER) {
    val serverSide by boolean("ServerSide", false)
}