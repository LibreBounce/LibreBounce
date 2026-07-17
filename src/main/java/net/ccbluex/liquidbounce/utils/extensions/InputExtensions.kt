/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.minecraft.client.entity.living.player.Input

fun Input.reset() {
    this.moveForward = 0f
    this.moveStrafe = 0f
    this.jump = false
    this.sneak = false
}

val Input.isSideways
    get() = moveForward != 0f && moveStrafe != 0f

val Input.isMoving
    get() = moveForward != 0f || moveStrafe != 0f