/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.NoWebMode
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.init.Blocks.web
import net.minecraft.network.packet.c2s.play.PlayerHandActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerHandActionC2SPacket.Action
import net.minecraft.util.math.Direction

object OldGrim : NoWebMode("OldGrim") {
    override fun onUpdate() {
        val searchBlocks = BlockUtils.searchBlocks(2, setOf(web))
        mc.player.isInWeb = false
        for (block in searchBlocks) {
            val blockpos = block.key
            sendPacket(PlayerHandActionC2SPacket(Action.STOP_DESTROY_BLOCK, blockpos, Direction.DOWN))
        }
    }
}
