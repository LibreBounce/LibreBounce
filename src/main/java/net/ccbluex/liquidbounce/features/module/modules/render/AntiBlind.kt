/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.value.bool
import net.ccbluex.liquidbounce.value.float

object AntiBlind : Module("AntiBlind", Category.RENDER, gameDetecting = false, hideModule = false) {
    val confusionEffect by bool("Confusion", true)
    val pumpkinEffect by bool("Pumpkin", true)
    val fireEffect by float("FireAlpha", 0.3f, 0f..1f)
    val bossHealth by bool("BossHealth", true)
}