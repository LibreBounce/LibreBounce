/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

fun String.toLowerCamelCase() = String(toCharArray().apply {
    this[0] = this[0].lowercaseChar()
})
