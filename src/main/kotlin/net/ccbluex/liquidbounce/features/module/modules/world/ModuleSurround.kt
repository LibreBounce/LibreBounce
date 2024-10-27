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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleCenter
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.block.placer.BlockPlacer
import net.ccbluex.liquidbounce.utils.block.placer.CrystalDestroyFeature
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.getSlot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import kotlin.math.ceil

/**
 * Surround module
 *
 * Builds safe holes.
 */
object ModuleSurround : Module("Surround", Category.WORLD, disableOnQuit = true) {

    private val DEFAULT_BLOCKS = hashSetOf(Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.CRYING_OBSIDIAN)

    private val center by boolean("Center", false)

    /**
     * Extends when entities block placement spots.
     */
    private val extend by boolean("Extend", true)

    /**
     * Places blocks bellow the surround so that enemies can't mine the block bellow you making you fall down.
     */
    private val down by boolean("Down", true)

    // TODO x/z distance or movement speed disable
    private val disableOnYChange by boolean("DisableOnYChange", true)

    // TODO instant

    /**
     * Requires the crystal destroyer in the placer to be active.
     */
    private object Protect : ToggleableConfigurable(this, "Protect", true) {

        val minDestroyProgress by int("MinDestroyProgress", 4, 0..9, "stage")

        /**
         * With a higher priority so that it runs before [CrystalDestroyFeature].
         */
        @Suppress("unused")
        private val tickHandler = handler<GameTickEvent>(priority = 10) {
            if (!placer.crystalDestroyer.enabled) {
                return@handler
            }

            placer.blocks.filter { !it.value }.keys.forEach { pos ->
                val breakingProgressions = mc.worldRenderer.blockBreakingProgressions[pos.asLong()] ?: return@forEach

                val breakingInfo = breakingProgressions.lastOrNull { it.actorId != player.id } ?: return@forEach
                if (breakingInfo.stage < minDestroyProgress) {
                    return@forEach
                }

                val blockedResult = pos.isBlockedByEntitiesReturnCrystal()
                val crystal = blockedResult.value() ?: return@forEach

                placer.crystalDestroyer.currentTarget = crystal
                if (placer.crystalDestroyer.currentTarget == crystal) {
                    return@handler
                }
            }
        }

    }

    init {
        tree(Protect)
    }

    private val filter by enumChoice("Filter", Filter.WHITELIST)
    private val blocks by blocks("Blocks", DEFAULT_BLOCKS)
    private val placer = tree(BlockPlacer("Placing", this, Priority.NORMAL, { filter.getSlot(blocks) }))

    private var startY = 0.0

    init {
        placer.support.blocks.addAll(DEFAULT_BLOCKS)
    }

    override fun enable() {
        if (center) {
            ModuleCenter.enabled = true
        }

        startY = player.pos.y
    }

    @Suppress("unused")
    private val tickMoveHandler = handler<PlayerNetworkMovementTickEvent> {
        if (it.state == EventState.PRE) {
            return@handler
        }

        if (disableOnYChange && it.y > 0.0) {
            enabled = false
        }
    }

    @Suppress("unused")
    private val targetUpdater = handler<SimulatedTickEvent> {
        if (disableOnYChange && player.pos.y != startY) {
            enabled = false
            return@handler
        }

        val bb = player.boundingBox
        val y = ceil(bb.minY)

        val hole = setOf(
            BlockPos.ofFloored(bb.minX, y, bb.minZ),
            BlockPos.ofFloored(bb.minX, y, bb.maxZ),
            BlockPos.ofFloored(bb.maxX, y, bb.minZ),
            BlockPos.ofFloored(bb.maxX, y, bb.maxZ),
        )

        val holeBlocks = hashSetOf<BlockPos>()

        hole.forEach {
            // the block is already placed, this could happen when we stand in a corner
            if (it.isBlastResistant()) {
                return@forEach
            }

            DIRECTIONS_EXCLUDING_UP.forEach { direction ->
                holeBlocks.add(it.offset(direction))

                if (direction == Direction.DOWN && down) {
                    holeBlocks.add(it.offset(direction, 2))
                }
            }
        }

        if (extend) {
            // TODO filter out blocked by crystals
            holeBlocks.filter { it.isBlockedByEntities() }.forEach {

            }
        }

        placer.update(holeBlocks)
    }

    override fun disable() {
        placer.disable()
    }

}
