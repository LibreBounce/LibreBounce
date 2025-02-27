/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.event

class EventHook<T : Event>(
    val owner: Listenable,
    val always: Boolean = false,
    val priority: Byte = 0,
    val action: (T) -> Unit
) {
    val isActive: Boolean
        get() = this.owner.handleEvents() || this.always
}
