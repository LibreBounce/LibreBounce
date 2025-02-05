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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PlayerMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.placer.BlockPlacer
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.getBlock
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import kotlin.random.Random

/**
 * BlockIn module
 *
 * Builds blocks to cover yourself.
 */
object ModuleBlockIn : ClientModule("BlockIn", Category.WORLD, disableOnQuit = true) {
    private val blockPlacer = tree(BlockPlacer("Placer", this, Priority.NORMAL, ::slotFinder))
    private val autoDisable by boolean("AutoDisable", true)
    private val filter by enumChoice("Filter", Filter.BLACKLIST)
    private val blocks by blocks("Blocks", hashSetOf())

    private val startPos = BlockPos.Mutable()
    private var rotateClockwise = false
    private var blockList = emptySet<BlockPos>()

    override fun disable() {
        startPos.set(BlockPos.ORIGIN)
        blockList = emptySet()
        blockPlacer.disable()
    }

    override fun enable() {
        startPos.set(player.blockPos)
        rotateClockwise = Random.nextBoolean()
        getPositions()
    }

    private fun Direction.next() = if (rotateClockwise) {
        rotateYClockwise()
    } else {
        rotateYCounterclockwise()
    }

    private fun getPositions() {
        // Rotate clockwise from player facing
        blockList = sequence<BlockPos> {
            var direction = player.horizontalFacing

            repeat(4) {
                val value = startPos.offset(direction)
                yield(value)
                yield(value.up())
                direction = direction.next()
            }

            yield(startPos.up(2))
        }.filterTo(linkedSetOf()) { // keep order
            it.getState()!!.isReplaceable
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        blockPlacer.update(blockList)
        waitUntil(blockPlacer::isDone)

        if (autoDisable) {
            notification(name, message("filled"), NotificationEvent.Severity.SUCCESS)
            enabled = false
        }
        getPositions()
    }

    @Suppress("unused")
    private val movementHandler = handler<PlayerMovementTickEvent> {
        val currentPos = player.blockPos

        if (currentPos != startPos && currentPos != startPos.up()) {
            notification(name, message("positionChanged"), NotificationEvent.Severity.ERROR)
            enabled = false
        }
    }

    private fun slotFinder(pos: BlockPos?): HotbarItemSlot? {
        val blockSlots = Slots.Hotbar.mapNotNull {
            it to (it.itemStack.getBlock()?.takeIf { b -> filter(b, blocks) } ?: return@mapNotNull null)
        }

        return if (pos in blockList) {
            blockSlots.maxByOrNull { (_, block) -> block.hardness }
        } else {
            blockSlots.minByOrNull { (_, block) -> block.hardness }
        }?.first
    }

}
