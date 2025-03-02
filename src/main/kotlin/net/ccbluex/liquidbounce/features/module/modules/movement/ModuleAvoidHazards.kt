/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.events.BlockShapeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.minecraft.block.*
import net.minecraft.fluid.Fluids
import net.minecraft.util.shape.VoxelShapes

/**
 * Anti hazards module
 *
 * Prevents you walking into blocks that might be malicious for you.
 */
object ModuleAvoidHazards : ClientModule("AvoidHazards", Category.MOVEMENT) {

    private val cacti by boolean("Cacti", true)
    private val berryBush by boolean("BerryBush", true)
    private val pressurePlates by boolean("PressurePlates", true)
    private val fire by boolean("Fire", true)
    private val lava by boolean("Lava", true)
    private val magmaBlocks by boolean("MagmaBlocks", true)

    // Conflicts with AvoidHazards
    val cobWebs by boolean("Cobwebs", true)

    val UNSAFE_BLOCK_CAP = Block.createCuboidShape(
        0.0,
        0.0,
        0.0,
        16.0,
        4.0,
        16.0
    )

    @Suppress("unused")
    val shapeHandler = handler<BlockShapeEvent> { event ->
        val block = event.state.block
        val fluidState = event.state.fluidState

        event.shape = when {
            block is CactusBlock && cacti -> VoxelShapes.fullCube()
            block is SweetBerryBushBlock && berryBush -> VoxelShapes.fullCube()
            block is FireBlock && fire -> VoxelShapes.fullCube()
            block is CobwebBlock && cobWebs -> VoxelShapes.fullCube()
            block is AbstractPressurePlateBlock && pressurePlates -> UNSAFE_BLOCK_CAP
            event.pos.down().getBlock() is MagmaBlock && magmaBlocks -> UNSAFE_BLOCK_CAP
            (fluidState.isOf(Fluids.LAVA) || fluidState.isOf(Fluids.FLOWING_LAVA)) && lava -> VoxelShapes.fullCube()
            else -> return@handler
        }
    }

}
