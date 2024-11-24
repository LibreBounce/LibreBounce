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
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoTool
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.MineMode
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.minecraft.block.BlockState
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

object CivMineMode : MineMode("Civ", stopOnStateChange = false) {

    private val switch by boolean("Switch", false)

    override fun isInvalid(blockPos: BlockPos, state: BlockState): Boolean {
        return state.getHardness(world, blockPos) == 1f && !player.isCreative
    }

    override fun onCannotLookAtTarget(blockPos: BlockPos) {
        // send always a packet to keep the target
        network.sendPacket(PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
            blockPos,
            Direction.DOWN
        ))
    }

    override fun shouldTarget(blockPos: BlockPos, state: BlockState): Boolean {
        return state.getHardness(world, blockPos) > 0f
    }

    override fun start(blockPos: BlockPos, direction: Direction?) {
        NormalMineMode.start(blockPos, direction)
    }

    override fun finish(blockPos: BlockPos, direction: Direction) {
        network.sendPacket(PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction))
        ModulePacketMine.finished = true
    }

    override fun shouldUpdate(
        blockPos: BlockPos,
        direction: Direction,
        slot: IntObjectImmutablePair<ItemStack>?
    ): Boolean {
        if (!ModulePacketMine.finished) {
            return true
        }

        // some blocks only break when holding a certain tool
        val oldSlot = player.inventory.selectedSlot
        val state = world.getBlockState(blockPos)
        var shouldSwitch = switch && state.isToolRequired
        if (shouldSwitch && ModuleAutoTool.enabled) {
            ModuleAutoTool.switchToBreakBlock(blockPos)
            shouldSwitch = false
        } else if (shouldSwitch) {
            val slot1 = findHotbarSlot { stack -> stack.isSuitableFor(state) } ?: -1
            if (slot1 != -1 && slot1 != oldSlot) {
                network.sendPacket(UpdateSelectedSlotC2SPacket(slot1))
            } else {
                shouldSwitch = false
            }
        }

        // Alright, for some reason when we spam STOP_DESTROY_BLOCK
        // server accepts us to destroy the same block instantly over and over.
        network.sendPacket(PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction))

        if (shouldSwitch) {
            network.sendPacket(UpdateSelectedSlotC2SPacket(oldSlot))
        }

        return false
    }

}
