/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance

open class LiquidWalkMode(val modeName: String) : MinecraftInstance {
    open fun onMove(event: MoveEvent) {}
    open fun onUpdate(event: UpdateEvent) {}
    open fun onPacket(event: PacketEvent) {}
    open fun onBlockBB(event: BlockBBEvent) {}
    open fun onJump(event: JumpEvent) {}

    open fun onEnable() {}
    open fun onDisable() {}
}