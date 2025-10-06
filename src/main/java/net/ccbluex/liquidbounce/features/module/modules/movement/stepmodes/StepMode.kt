/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.StepEvent
import net.ccbluex.liquidbounce.event.StepConfirmEvent
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance

open class StepMode(val modeName: String) : MinecraftInstance {
    open fun onMotion() {}
    open fun onUpdate() {}
    open fun onMove(event: MoveEvent) {}
    open fun onStep(event: StepEvent) {}
    open fun onStepConfirm(event: StepConfirmEvent) {}
    open fun onStrafe() {}
    open fun onJump(event: JumpEvent) {}
    open fun onPacket(event: PacketEvent) {}
    open fun onEnable() {}
    open fun onDisable() {}
}