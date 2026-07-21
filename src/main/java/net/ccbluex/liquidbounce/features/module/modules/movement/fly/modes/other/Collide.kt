/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.other

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode
import net.ccbluex.liquidbounce.utils.client.pos
import net.ccbluex.liquidbounce.utils.extensions.offset
import net.minecraft.block.LadderBlock
import net.minecraft.block.material.Material
import net.minecraft.util.math.Box

object Collide : FlyMode("Collide") {
    override fun onBB(event: BlockBBEvent) {
        if (!mc.gameOptions.jumpKey.isKeyDown && mc.gameOptions.sneakKey.isKeyDown) return

        if (!event.block.material.blocksMovement() && event.block.material != Material.carpet && event.block.material != Material.vine && event.block.material != Material.snow && event.block !is LadderBlock) {
            event.boundingBox = Box(-2.0, -1.0, -2.0, 2.0, 1.0, 2.0).offset(event.pos)
        }
    }
}
