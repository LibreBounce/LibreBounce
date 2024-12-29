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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.MixinWorldRenderer
import net.ccbluex.liquidbounce.render.BoxRenderer
import net.ccbluex.liquidbounce.render.drawBoxSide
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.math.Easing
import net.minecraft.block.ShapeContext
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.shape.VoxelShape

/**
 * Block Outline module
 *
 * Changes the way Minecraft highlights blocks.
 *
 * TODO: Implement GUI Information Panel
 *
 * [MixinWorldRenderer.cancelBlockOutline]
 */
object ModuleBlockOutline : ClientModule("BlockOutline", Category.RENDER, aliases = arrayOf("BlockOverlay")) {

    private val sideOnly by boolean("SideOnly", true)
    private val color by color("Color", Color4b(68, 117, 255, 70))
    private val outlineColor by color("Outline", Color4b(68, 117, 255, 150))

    private object Slide : ToggleableConfigurable(this, "Slide", true) {
        val time by int("Time", 150, 1..1000, "ms")
        val easing by curve("Easing", Easing.LINEAR)
    }

    init {
        tree(Slide)
    }

    private var currentPosition: Box? = null
    private var previousPosition: Box? = null
    private var lastChange = 0L

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val target = mc.crosshairTarget
        if (target !is BlockHitResult || target.getType() == HitResult.Type.MISS) {
            resetPositions()
            return@handler
        }

        val blockPos = target.blockPos
        val blockState = world.getBlockState(blockPos)
        if (blockState.isAir || !world.worldBorder.contains(blockPos)) {
            resetPositions()
            return@handler
        }

        val side = target.side
        val box = blockState.getOutlineShape(this.world, blockPos, ShapeContext.of(mc.cameraEntity))
        val finalPosition = (if (sideOnly) flatBox(box, side) else box.boundingBox).offset(blockPos)
        if (currentPosition != finalPosition) {
            previousPosition = currentPosition
            currentPosition = finalPosition
            lastChange = System.currentTimeMillis()
        }

        val renderPosition = if (previousPosition != null && Slide.running) {
            val factor = Slide.easing.getFactor(
                lastChange,
                System.currentTimeMillis(),
                Slide.time.toFloat()
            ).toDouble()

            val previousPosition1 = previousPosition!!
            Box(
                MathHelper.lerp(factor, previousPosition1.minX, finalPosition.minX),
                MathHelper.lerp(factor, previousPosition1.minY, finalPosition.minY),
                MathHelper.lerp(factor, previousPosition1.minZ, finalPosition.minZ),
                MathHelper.lerp(factor, previousPosition1.maxX, finalPosition.maxX),
                MathHelper.lerp(factor, previousPosition1.maxY, finalPosition.maxY),
                MathHelper.lerp(factor, previousPosition1.maxZ, finalPosition.maxZ)
            )
        } else {
            finalPosition
        }

        val translatedPosition = renderPosition.offset(mc.entityRenderDispatcher.camera.pos.negate())
        renderEnvironmentForWorld(event.matrixStack) {
            if (sideOnly) {
                drawBoxSide(translatedPosition, side, color, outlineColor)
            } else {
                BoxRenderer.drawWith(this) {
                    drawBox(translatedPosition, color, outlineColor)
                }
            }
        }
    }

    private fun flatBox(shape: VoxelShape, side: Direction) = when (side) {
        Direction.UP -> Box(
            shape.getMin(Direction.Axis.X),
            shape.getMax(Direction.Axis.Y),
            shape.getMin(Direction.Axis.Z),
            shape.getMax(Direction.Axis.X),
            shape.getMax(Direction.Axis.Y),
            shape.getMax(Direction.Axis.Z)
        )
        Direction.DOWN -> Box(
            shape.getMin(Direction.Axis.X),
            shape.getMin(Direction.Axis.Y),
            shape.getMin(Direction.Axis.Z),
            shape.getMax(Direction.Axis.X),
            shape.getMin(Direction.Axis.Y),
            shape.getMax(Direction.Axis.Z)
        )
        Direction.NORTH -> Box(
            shape.getMin(Direction.Axis.X),
            shape.getMin(Direction.Axis.Y),
            shape.getMin(Direction.Axis.Z),
            shape.getMax(Direction.Axis.X),
            shape.getMax(Direction.Axis.Y),
            shape.getMin(Direction.Axis.Z)
        )
        Direction.SOUTH -> Box(
            shape.getMin(Direction.Axis.X),
            shape.getMin(Direction.Axis.Y),
            shape.getMax(Direction.Axis.Z),
            shape.getMax(Direction.Axis.X),
            shape.getMax(Direction.Axis.Y),
            shape.getMax(Direction.Axis.Z)
        )
        Direction.WEST -> Box(
            shape.getMin(Direction.Axis.X),
            shape.getMin(Direction.Axis.Y),
            shape.getMin(Direction.Axis.Z),
            shape.getMin(Direction.Axis.X),
            shape.getMax(Direction.Axis.Y),
            shape.getMax(Direction.Axis.Z)
        )
        Direction.EAST -> Box(
            shape.getMax(Direction.Axis.X),
            shape.getMin(Direction.Axis.Y),
            shape.getMin(Direction.Axis.Z),
            shape.getMax(Direction.Axis.X),
            shape.getMax(Direction.Axis.Y),
            shape.getMax(Direction.Axis.Z)
        )
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        resetPositions()
        lastChange = System.currentTimeMillis()
    }

    private fun resetPositions() {
        currentPosition = null
        previousPosition = null
    }

}
