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
package net.ccbluex.liquidbounce.features.module.modules.world.nuker

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.area.FloorNukerArea
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.area.SphereNukerArea
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.mode.InstantNukerMode
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.mode.LegitNukerMode
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
import net.minecraft.util.math.BlockPos

/**
 * Nuker module
 *
 * Destroys blocks around you.
 */
object ModuleNuker : Module("Nuker", Category.WORLD, disableOnQuit = true) {

    val mode =
        choices("Mode", LegitNukerMode, arrayOf(LegitNukerMode, InstantNukerMode))
    val areaMode = choices(
        "AreaMode",
        SphereNukerArea,
        arrayOf(SphereNukerArea, FloorNukerArea)
    )

    var swingMode by enumChoice("Swing", SwingMode.DO_NOT_HIDE)
    val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    private val targetRenderer = tree(
        PlacementRenderer("TargetRendering", true, this)
    )

    /**
     * The last target block that was hit. Does not mean it was destroyed.
     */
    var wasTarget: BlockPos? = null
        set(value) {
            field = value
            targetRenderer.addBlock(value ?: return)
        }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent>(priority = EventPriorityConvention.FIRST_PRIORITY) {
        // I could not find a better way of removing the blocks from the renderer.
        targetRenderer.clearSilently()
    }

    override fun disable() {
        wasTarget = null
        super.disable()
    }

}
