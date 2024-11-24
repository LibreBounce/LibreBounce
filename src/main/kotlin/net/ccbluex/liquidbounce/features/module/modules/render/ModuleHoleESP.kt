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

import it.unimi.dsi.fastutil.longs.Long2ByteMap
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap
import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerPostTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.MovableRegionScanner
import net.ccbluex.liquidbounce.utils.block.Region
import net.ccbluex.liquidbounce.utils.block.Region.Companion.getBox
import net.ccbluex.liquidbounce.utils.kotlin.getValue
import net.ccbluex.liquidbounce.utils.kotlin.isEmpty
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.registry.Registries
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.chunk.Chunk
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.math.max

private const val UNBREAKABLE = (-1).toByte()
private const val AIR = 0.toByte()
private const val BREAKABLE = 1.toByte()

// BlockState types
typealias State = Byte

/**
 * HoleESP module
 *
 * Detects and displays safe spots for Crystal PvP.
 */

object ModuleHoleESP : Module("HoleESP", Category.RENDER) {

    private val modes = choices("Mode", GlowingPlane, arrayOf(BoxChoice, GlowingPlane))

    private val horizontalDistance by int("HorizontalScanDistance", 32, 4..128)
    private val verticalDistance by int("VerticalScanDistance", 8, 4..128)

    private val distanceFade by float("DistanceFade", 0.3f, 0f..1f)

    private val color1by1 by color("1x1", Color4b(0x19c15c))
    private val color1by2 by color("1x2", Color4b(0x35bacc))
    private val color2by2 by color("2x2", Color4b(0xf7381b))

    private val movableRegionScanner = MovableRegionScanner()

    private val UNBREAKABLE_BLOCKS: Set<Block> by lazy {
        Registries.BLOCK.filterTo(hashSetOf()) { it.blastResistance >= 600 }
    }

    override fun disable() {
        ChunkScanner.unsubscribe(HoleTracker)
        movableRegionScanner.clearRegion()
    }

    override fun enable() {
        ChunkScanner.subscribe(HoleTracker)
        mc.player?.blockPos?.let(::updateScanRegion)
    }

    private object BoxChoice : Choice("Box") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val outline by boolean("Outline", true)

        @Suppress("unused")
        val renderHandler = handler<WorldRenderEvent> { event ->
            renderEnvironmentForWorld(event.matrixStack) {
                HoleTracker.holes.forEach {
                    val (type, positions) = it

                    val fade = calculateFade(positions.from)

                    val baseColor = type.color().alpha(50).fade(fade)

                    val box = positions.getBox()
                    withPositionRelativeToCamera(positions.from.toVec3d()) {
                        withColor(baseColor) {
                            drawSolidBox(box)
                        }

                        if (outline) {
                            val outlineColor = type.color().alpha(100).fade(fade)
                            withColor(outlineColor) {
                                drawOutlinedBox(box)
                            }
                        }
                    }
                }
            }
        }
    }

    private object GlowingPlane : Choice("GlowingPlane") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val outline by boolean("Outline", true)

        private val glowHeightSetting by float("GlowHeight", 0.7f, 0f..1f)

        @Suppress("unused")
        val renderHandler = handler<WorldRenderEvent> { event ->
            val glowHeight = glowHeightSetting.toDouble()

            renderEnvironmentForWorld(event.matrixStack) {
                withDisabledCull {
                    HoleTracker.holes.forEach {
                        val (type, positions) = it

                        val fade = calculateFade(positions.from)

                        val baseColor = type.color().alpha(50).fade(fade)
                        val transparentColor = baseColor.alpha(0)

                        val box = positions.getBox()
                        withPositionRelativeToCamera(positions.from.toVec3d()) {
                            withColor(baseColor) {
                                drawSideBox(box, Direction.DOWN)
                            }

                            if (outline) {
                                val outlineColor = type.color().alpha(100).fade(fade)
                                withColor(outlineColor) {
                                    drawSideBox(box, Direction.DOWN, onlyOutline = true)
                                }
                            }

                            drawGradientSides(glowHeight, baseColor, transparentColor, box)
                        }
                    }
                }
            }
        }
    }

    private val playerPos = BlockPos.Mutable()

    @Suppress("unused")
    val movementHandler = handler<PlayerPostTickEvent> {
        val currentPos = player.blockPos

        if (playerPos.getManhattanDistance(currentPos) >= 4) {
            updateScanRegion(currentPos)
        }
    }

    private fun updateScanRegion(newPlayerPos: BlockPos) {
        playerPos.set(newPlayerPos)

        val changedAreas = movableRegionScanner.moveTo(
            Region.quadAround(
                playerPos,
                horizontalDistance,
                verticalDistance
            )
        )

        if (changedAreas.isEmpty()) {
            return
        }

        val region = movableRegionScanner.currentRegion

        with(HoleTracker) {
            // Remove blocks out of the area
            holes.removeIf { !it.positions.intersects(region) }

            // Update new area
            changedAreas.forEach {
                it.cachedUpdate()
            }
        }
    }

    private fun calculateFade(pos: BlockPos): Float {
        if (distanceFade == 0f)
            return 1f

        val verticalDistanceFraction = (player.pos.y - pos.y) / verticalDistance
        val horizontalDistanceFraction =
            Vec3d(player.pos.x - pos.x, 0.0, player.pos.z - pos.z).length() / horizontalDistance

        val fade = (1 - max(verticalDistanceFraction, horizontalDistanceFraction)) / distanceFade

        return fade.coerceIn(0.0, 1.0).toFloat()
    }

    @JvmRecord
    private data class Hole(
        val type: Type,
        val positions: Region,
        val blockInvalidators: Region = Region(positions.from, positions.to.up(2)),
    ) : Comparable<Hole> {
        override fun compareTo(other: Hole): Int =
            compareValuesBy(this, other) { it.positions.from }

        operator fun contains(pos: BlockPos): Boolean = pos in positions

        enum class Type(val size: Int, val color: () -> Color4b) {
            ONE_ONE(1, { color1by1 }),
            ONE_TWO(2, { color1by2 }),
            TWO_TWO(4, { color2by2 }),
        }
    }

    private object HoleTracker : ChunkScanner.BlockChangeSubscriber {
        val holes = ConcurrentSkipListSet<Hole>()

        private val mutable by ThreadLocal.withInitial(BlockPos::Mutable)

        private val fullSurroundings = setOf(Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH)

        override val shouldCallRecordBlockOnChunkUpdate: Boolean
            get() = false

        override fun recordBlock(pos: BlockPos, state: BlockState, cleared: Boolean) {
            // Invalidate old ones
            if (state.isAir) {
                // if one of the neighbor blocks becomes air, invalidate the hole
                holes.removeIf { it.positions.any { p -> p.getManhattanDistance(pos) == 1 } }
            } else {
                holes.removeIf { pos in it.blockInvalidators }
            }

            // Check new ones
            val region = Region(pos.add(-2, -3, -2), pos.add(2, 3, 2))
            invalidate(region)
            region.cachedUpdate()
        }

        private fun invalidate(region: Region) {
            holes.removeIf { it.positions.intersects(region) }
        }

        @Suppress("detekt:CognitiveComplexMethod")
        fun Region.cachedUpdate(chunk: Chunk? = null) {
            val buffer = Long2ByteOpenHashMap(volume)

            // Only check positions in this chunk (pos is BlockPos.Mutable)
            forEach { pos ->
                if (chunk != null && (pos.y <= chunk.bottomY || pos.y - 1 >= chunk.topY)) {
                    return@forEach
                }

                if (holes.any { pos in it } || !buffer.checkSameXZ(pos)) {
                    return@forEach
                }

                val surroundings = fullSurroundings.filterTo(HashSet(4, 1.0F)) { direction ->
                    buffer.cache(mutable.set(pos, direction)) == UNBREAKABLE
                }

                when (surroundings.size) {
                    // 1*1
                    4 -> holes += Hole(
                        Hole.Type.ONE_ONE,
                        Region.from(pos),
                    )
                    // 1*2
                    3 -> {
                        val airDirection = fullSurroundings.first { it !in surroundings }
                        val another = pos.offset(airDirection)

                        if (!buffer.checkSameXZ(another)) {
                            return@forEach
                        }

                        val airOpposite = airDirection.opposite
                        val checkDirections = with(fullSurroundings.iterator()) {
                            Array(3) {
                                val value = next()
                                if (value == airOpposite) next() else value
                            }
                        }

                        if (buffer.checkSurroundings(another, checkDirections)) {
                            holes += Hole(
                                Hole.Type.ONE_TWO,
                                Region(pos, another),
                            )
                        }
                    }
                    // 2*2
                    2 -> {
                        val (direction1, direction2) = fullSurroundings.filterTo(ArrayList(2)) { it !in surroundings }

                        val mutableLocal = BlockPos.Mutable()

                        if (!buffer.checkState(mutableLocal.set(pos, direction1), direction1, direction2.opposite)) {
                            return@forEach
                        }

                        if (!buffer.checkState(mutableLocal.set(pos, direction2), direction2, direction1.opposite)) {
                            return@forEach
                        }

                        if (!buffer.checkState(mutableLocal.move(direction1), direction1, direction2)) {
                            return@forEach
                        }

                        holes += Hole(
                            Hole.Type.TWO_TWO,
                            Region(pos, mutableLocal),
                        )
                    }
                }
            }
        }

        private fun Long2ByteMap.cache(blockPos: BlockPos): State {
            val longValue = blockPos.asLong()
            if (containsKey(longValue)) {
                return get(longValue)
            } else {
                val state = mc.world?.getBlockState(blockPos) ?: return AIR
                val result = when {
                    state.isAir -> AIR
                    state.block in UNBREAKABLE_BLOCKS -> UNBREAKABLE
                    else -> BREAKABLE
                }
                put(longValue, result)
                return result
            }
        }

        private fun Long2ByteMap.checkSameXZ(blockPos: BlockPos): Boolean {
            mutable.set(blockPos.x, blockPos.y - 1, blockPos.z)
            if (cache(mutable) != UNBREAKABLE) {
                return false
            }

            repeat(3) {
                mutable.y++
                if (cache(mutable) != AIR) {
                    return false
                }
            }

            return true
        }

        private fun Long2ByteMap.checkSurroundings(
            blockPos: BlockPos,
            directions: Array<out Direction>
        ): Boolean {
            return directions.all { cache(mutable.set(blockPos, it)) == UNBREAKABLE }
        }

        private fun Long2ByteMap.checkState(
            blockPos: BlockPos,
            vararg directions: Direction
        ): Boolean {
            return checkSameXZ(blockPos) && checkSurroundings(blockPos, directions)
        }

        override fun chunkUpdate(x: Int, z: Int) {
            val chunk = mc.world?.getChunk(x, z) ?: return
            val region = Region.from(chunk)
            if (region.intersects(movableRegionScanner.currentRegion)) {
                invalidate(region)
                region.cachedUpdate(chunk)
            }
        }

        override fun clearChunk(x: Int, z: Int) {
            invalidate(Region.fromChunkPos(x, z))
        }

        override fun clearAllChunks() {
            holes.clear()
        }

    }

}
