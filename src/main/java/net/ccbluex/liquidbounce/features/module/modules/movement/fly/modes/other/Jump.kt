/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.other

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.Fly.jumpY
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.block.LadderBlock
import net.minecraft.block.material.Material
import net.minecraft.util.math.Box

object Jump : FlyMode("Jump") {

    override fun onUpdate() {
        mc.player?.run {
            if (onGround && !jumping)
                tryJump()

            if (onGround ||
                mc.gameOptions.jumpKey.isKeyDown && !mc.gameOptions.sneakKey.isKeyDown
            )
                jumpY = posY
        }
    }

    override fun onBB(event: BlockBBEvent) {
        val jumpYCondition =
            if (!mc.gameOptions.jumpKey.isKeyDown && mc.gameOptions.sneakKey.isKeyDown) event.y.toDouble() < jumpY else event.y.toDouble() <= jumpY

        if ((!event.block.material.blocksMovement() && event.block.material != Material.carpet && event.block.material != Material.vine && event.block.material != Material.snow && event.block !is LadderBlock) && jumpYCondition) {
            event.boundingBox = Box.fromBounds(
                event.x.toDouble(),
                event.y.toDouble(),
                event.z.toDouble(),
                event.x.toDouble() + 1,
                1.0,
                event.z.toDouble() + 1
            )
        }
    }
}
