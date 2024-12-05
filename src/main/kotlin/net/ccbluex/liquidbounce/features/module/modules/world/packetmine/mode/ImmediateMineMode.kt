/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.world.packetmine.mode

import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.MineMode
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

object ImmediateMineMode : MineMode("Immediate", canManuallyChange = false, canAbort = false) {

    private val waitForConfirm by boolean("WaitForConfirm", true)

    override fun start(blockPos: BlockPos, direction: Direction?) {
        NormalMineMode.start(blockPos, direction)
        network.sendPacket(
            PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction)
        )
    }

    override fun finish(blockPos: BlockPos, direction: Direction) {
        if (!waitForConfirm) {
            ModulePacketMine.finished = true
            ModulePacketMine._resetTarget()
        }
    }

    override fun shouldUpdate(
        blockPos: BlockPos,
        direction: Direction,
        slot: IntObjectImmutablePair<ItemStack>?
    ): Boolean {
        return ModulePacketMine.progress < 1f
    }

}
