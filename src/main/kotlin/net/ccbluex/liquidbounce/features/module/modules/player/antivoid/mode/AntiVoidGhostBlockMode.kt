/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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
 *
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.player.antivoid.mode

import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.BlockShapeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.ModuleAntiVoid
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.ModuleAntiVoid.isLikelyFalling
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.ModuleAntiVoid.isTestingCollision
import net.minecraft.util.shape.VoxelShapes

object AntiVoidGhostBlockMode : AntiVoidMode("GhostBlock") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleAntiVoid.mode

    @Suppress("unused")
    private val handleBlockShape = handler<BlockShapeEvent> { event ->
        // Otherwise we would be stuck in a loop
        if (isTestingCollision || event.shape != VoxelShapes.empty() || !isLikelyFalling || isExempt) {
            return@handler
        }

        val position = event.pos

        // We only want to place a block below the player
        if (position.y >= player.blockY) {
            return@handler
        }

        event.shape = VoxelShapes.fullCube()
    }

    /**
     * We do not use the general logic of AntiVoid here, we have a very basic one here.
     */
    override fun fix() = false

}
