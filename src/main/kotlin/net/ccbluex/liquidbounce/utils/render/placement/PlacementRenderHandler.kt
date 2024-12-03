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
package net.ccbluex.liquidbounce.utils.render.placement

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper

// TODO check whether the Boxes actually touch
/**
 * A renderer instance that can be added to a [PlacementRenderer], it contains the core logic.
 * Culling is handled in each handler for its boxes individually.
 */
class PlacementRenderHandler(private val placementRenderer: PlacementRenderer, val id: Int = 0) {

    private val inList = Long2ObjectLinkedOpenHashMap<InOutBlockData>()
    private val currentList = Long2ObjectLinkedOpenHashMap<CurrentBlockData>()
    private val outList = Long2ObjectLinkedOpenHashMap<InOutBlockData>()

    @JvmRecord
    private data class InOutBlockData(val startTime: Long, val cullData: Long, val box: Box) {
        fun toCurrent() = CurrentBlockData(cullData, box)
    }

    @JvmRecord
    private data class CurrentBlockData(val cullData: Long, val box: Box) {
        fun toInOut(startTime: Long) = InOutBlockData(startTime, cullData, box)
    }

    private val blockPosCache = BlockPos.Mutable()

    fun render(event: WorldRenderEvent, time: Long) {
        val matrixStack = event.matrixStack

        with(placementRenderer) {
            val color = getColor(id)
            val outlineColor = getOutlineColor(id)

            renderEnvironmentForWorld(matrixStack) {
                BoxRenderer.drawWith(this) {
                    fun drawEntryBox(blockPos: BlockPos, cullData: Long, box: Box, colorFactor: Float) {
                        withPositionRelativeToCamera(blockPos.toVec3d()) {
                            drawBox(
                                box,
                                color.fade(colorFactor),
                                outlineColor.fade(colorFactor),
                                (cullData shr 32).toInt(),
                                (cullData and 0xFFFFFFFF).toInt()
                            )
                        }
                    }

                    inList.long2ObjectEntrySet().iterator().apply {
                        while (hasNext()) {
                            val (pos, value) = next()

                            val sizeFactor = startSizeCurve.getFactor(value.startTime, time, inTime.toFloat())
                            val expand = MathHelper.lerp(sizeFactor, startSize, 1f)
                            val box = getBox(if (expand < 1f) 1f - expand else expand, value.box)
                            val colorFactor = fadeInCurve.getFactor(value.startTime, time, inTime.toFloat())

                            drawEntryBox(blockPosCache.set(pos), value.cullData, box, colorFactor)

                            if (time - value.startTime >= outTime) {
                                if (keep) {
                                    currentList[pos] = value.toCurrent()
                                } else {
                                    outList[pos] = value.copy(startTime = time)
                                }
                                remove()
                            }
                        }
                    }

                    currentList.long2ObjectEntrySet().forEach {
                        val (pos, value) = it
                        drawEntryBox(blockPosCache.set(pos), value.cullData, value.box, 1f)
                    }

                    outList.long2ObjectEntrySet().iterator().apply {
                        while (hasNext()) {
                            val (pos, value) = next()

                            val sizeFactor = endSizeCurve.getFactor(value.startTime, time, outTime.toFloat())
                            val expand = 1f - MathHelper.lerp(sizeFactor, 1f, endSize)
                            val box = getBox(expand, value.box)
                            val colorFactor = 1f - fadeOutCurve.getFactor(value.startTime, time, outTime.toFloat())

                            drawEntryBox(blockPosCache.set(pos), value.cullData, box, colorFactor)

                            if (time - value.startTime >= outTime) {
                                remove()
                                updateNeighbors(blockPosCache.set(pos))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getBox(expand: Float, box: Box): Box {
        return when (expand) {
            1f -> box
            0f -> EMPTY_BOX
            else -> {
                val f = if (expand < 1) -0.5 * expand else (expand - 1) * 0.5
                box.expand(box.lengthX * f, box.lengthY * f, box.lengthZ * f)
            }
        }
    }

    fun isFinished(): Boolean = outList.isEmpty()

    /**
     * Updates the culling of all blocks around a position that has been removed or added.
     */
    fun updateNeighbors(pos: BlockPos) {
        if (!placementRenderer.clump) {
            return
        }

        // TODO in theory a one block radius should be enough
        pos.searchBlocksInCuboid(2).forEach {
            val longValue = it.asLong()

            if (inList.containsKey(longValue)) {
                inList.put(longValue, inList.get(longValue).copy(cullData = getCullData(it)))
                return@forEach
            }

            if (currentList.containsKey(longValue)) {
                currentList.put(longValue, currentList.get(longValue).copy(cullData = getCullData(it)))
                return@forEach
            }
        }
    }

    /**
     * Returns a long that stores in the first 32 bits what vertices are to be rendered for the faces and
     * in the other half what vertices are to be rendered for the outline.
     */
    private fun getCullData(pos: BlockPos): Long {
        var faces = 1 shl 30
        var edges = 1 shl 30

        val eastPos = pos.east()
        val westPos = pos.west()
        val upPos = pos.up()
        val downPos = pos.down()
        val southPos = pos.south()
        val northPos = pos.north()

        val east = contains(eastPos)
        val west = contains(westPos)
        val up = contains(upPos)
        val down = contains(downPos)
        val south = contains(southPos)
        val north = contains(northPos)

        faces = cullSide(faces, east, FACE_EAST)
        faces = cullSide(faces, west, FACE_WEST)
        faces = cullSide(faces, up, FACE_UP)
        faces = cullSide(faces, down, FACE_DOWN)
        faces = cullSide(faces, south, FACE_SOUTH)
        faces = cullSide(faces, north, FACE_NORTH)

        edges = cullEdge(edges, north, down, contains(northPos.down()), EDGE_NORTH_DOWN)
        edges = cullEdge(edges, east, down, contains(eastPos.down()), EDGE_EAST_DOWN)
        edges = cullEdge(edges, south, down, contains(southPos.down()), EDGE_SOUTH_DOWN)
        edges = cullEdge(edges, west, down, contains(westPos.down()), EDGE_WEST_DOWN)
        edges = cullEdge(edges, north, west, contains(northPos.west()), EDGE_NORTH_WEST)
        edges = cullEdge(edges, north, east, contains(northPos.east()), EDGE_NORTH_EAST)
        edges = cullEdge(edges, south, east, contains(southPos.east()), EDGE_SOUTH_EAST)
        edges = cullEdge(edges, south, west, contains(westPos.south()), EDGE_SOUTH_WEST)
        edges = cullEdge(edges, north, up, contains(northPos.up()), EDGE_NORTH_UP)
        edges = cullEdge(edges, east, up, contains(eastPos.up()), EDGE_EAST_UP)
        edges = cullEdge(edges, south, up, contains(southPos.up()), EDGE_SOUTH_UP)
        edges = cullEdge(edges, west, up, contains(westPos.up()), EDGE_WEST_UP)

        // combines the data in a single long and inverts it, so that all vertices that are to be rendered are
        // represented by 1s
        return ((faces.toLong() shl 32) or edges.toLong()).inv()
    }

    /**
     * Checks whether the position is rendered.
     */
    private fun contains(pos: BlockPos): Boolean {
        val longValue = pos.asLong()
        return inList.containsKey(longValue) || currentList.containsKey(longValue) || outList.containsKey(longValue)
    }

    /**
     * Applies a mask to the current data if either [direction1Present] and [direction2Present] are `false` or
     * [direction1Present] and [direction2Present] are `true` but [diagonalPresent] is `false`.
     *
     * This will result in the edge only being rendered if it's not surrounded by blocks and is on an actual
     * edge from multiple blocks seen as one entity.
     *
     * @return The updated [currentData]
     */
    private fun cullEdge(
        currentData: Int,
        direction1Present: Boolean,
        direction2Present: Boolean,
        diagonalPresent: Boolean,
        mask: Int
    ): Int {
        return if ((!direction1Present && !direction2Present)
            || (direction1Present && direction2Present && !diagonalPresent)) {
            currentData or mask
        } else {
            currentData
        }
    }

    /**
     * Applies a mask to the current data if either [directionPresent] is `false`.
     *
     * This will result in the face only being visible if it's on the outside of multiple blocks.
     *
     * @return The updated [currentData]
     */
    private fun cullSide(currentData: Int, directionPresent: Boolean, mask: Int): Int {
        return if (!directionPresent) {
            currentData or mask
        } else {
            currentData
        }
    }

    /**
     * Adds a block to be rendered. First it will make an appear-animation, then
     * it will continue to get rendered until it's removed or the world changes.
     */
    fun addBlock(pos: BlockPos, update: Boolean = true, box: Box = FULL_BOX) {
        val longValue = pos.asLong()
        if (!currentList.containsKey(longValue) && !inList.containsKey(longValue)) {
            inList.put(longValue, InOutBlockData(System.currentTimeMillis(), 0L, box))
            if (update) {
                updateNeighbors(pos)
            }
        }

        outList.remove(longValue)
    }

    /**
     * Removes a block from the rendering, it will get an out animation tho.
     */
    fun removeBlock(pos: BlockPos) {
        val longValue = pos.asLong()
        var cullData = 0L
        var box: Box? = null

        currentList.remove(longValue)?.let {
            cullData = it.cullData
            box = it.box
        } ?: run {
            inList.remove(longValue)?.let {
                cullData = it.cullData
                box = it.box
            } ?: return
        }

        outList.put(longValue, InOutBlockData(System.currentTimeMillis(), cullData, box!!))
    }

    /**
     * Updates all culling data.
     *
     * This can be useful to reduce overhead when adding a bunch of positions,
     * so that positions don't get updated multiple times.
     */
    fun updateAll() {
        inList.long2ObjectEntrySet().forEach { (key, value) ->
            inList.put(key, value.copy(cullData = getCullData(blockPosCache.set(key))))
        }

        currentList.long2ObjectEntrySet().forEach { (key, value) ->
            currentList.put(key, value.copy(cullData = getCullData(blockPosCache.set(key))))
        }
    }

    /**
     * Updates the box of [pos] to [box].
     *
     * This method won't affect positions that are in the state of fading out.
     */
    fun updateBox(pos: BlockPos, box: Box) {
        val longValue = pos.asLong()
        var needUpdate = false

        if (inList.containsKey(longValue)) {
            needUpdate = true
            inList.put(longValue, inList.get(longValue).copy(box = box))
        }

        if (currentList.containsKey(longValue)) {
            needUpdate = true
            currentList.put(longValue, currentList.get(longValue).copy(box = box))
        }

        if (needUpdate) {
            updateNeighbors(pos)
        }
    }

    /**
     * Puts all currently rendered positions in the out-animation state and keeps it being rendered until
     * all animations have been finished even though the module might be already disabled.
     */
    fun clearSilently() {
        inList.long2ObjectEntrySet().iterator().apply {
            while (hasNext()) {
                val (pos, value) = next()
                outList.put(pos, value.copy(startTime = System.currentTimeMillis()))
                remove()
            }
        }

        currentList.long2ObjectEntrySet().iterator().apply {
            while (hasNext()) {
                val (pos, value) = next()
                outList.put(pos, value.toInOut(startTime = System.currentTimeMillis()))
                remove()
            }
        }
    }

    /**
     * Removes all stored positions.
     */
    fun clear() {
        inList.clear()
        currentList.clear()
        outList.clear()
    }

}
