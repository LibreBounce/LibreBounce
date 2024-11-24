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
package net.ccbluex.liquidbounce.features.module.modules.world.packetmine

import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair
import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.utils.block.isBreakable
import net.ccbluex.liquidbounce.utils.block.isNotBreakable
import net.minecraft.block.BlockState
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

abstract class MineMode(
    name: String,
    val canManuallyChange: Boolean = true,
    val canAbort: Boolean = true,
    val stopNormalMining: Boolean = true,
    val stopOnStateChange: Boolean = true
) : Choice(name) {

    open fun isInvalid(blockPos: BlockPos, state: BlockState): Boolean {
        return state.isNotBreakable(blockPos) && !player.isCreative || state.isAir
    }

    open fun shouldTarget(blockPos: BlockPos, state: BlockState): Boolean {
        return state.isBreakable(blockPos)
    }

    open fun onCannotLookAtTarget(blockPos: BlockPos) {}

    abstract fun start(blockPos: BlockPos, direction: Direction?)

    abstract fun finish(blockPos: BlockPos, direction: Direction)

    abstract fun shouldUpdate(
        blockPos: BlockPos,
        direction: Direction,
        slot: IntObjectImmutablePair<ItemStack>?
    ): Boolean

    override val parent: ChoiceConfigurable<*>
        get() = ModulePacketMine.mode

}
