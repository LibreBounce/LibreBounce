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

import net.ccbluex.liquidbounce.event.events.BlockAttackEvent
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raytraceBlock
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
import net.minecraft.block.BlockState
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

/**
 * CivBreak module
 *
 * Allows you to break the same block faster.
 */
object ModuleCivBreak : Module("CivBreak", Category.WORLD) {

    private val rotate by boolean("Rotate", false)
    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)
    private val rotationsConfigurable = tree(RotationsConfigurable(this))
    private val switch by boolean("Switch", false)
    private val targetRenderer = tree(
        PlacementRenderer("TargetRendering", true, this,
            defaultColor = Color4b(255, 0, 0, 90),
            clump = false
        )
    )

    private val chronometer = Chronometer()
    private var targetPos: BlockPos? = null
        set(value) {
            field?.let { targetRenderer.removeBlock(it) }
            value?.let { targetRenderer.addBlock(it) }

            field = value
        }
    private var dir: Direction? = null

    init {
        handler<BlockAttackEvent> {
            if (it.pos.getState()!!.getHardness(world, it.pos) > 0f) {
                it.cancelEvent()
            }
        }
    }

    override fun enable() {
        interaction.cancelBlockBreaking()
        targetPos = ModulePacketMine.targetPos
        ModulePacketMine.targetPos = null
    }

    override fun disable() {
        targetPos = null
        dir = null
    }

    @Suppress("unused")
    private val repeatable = repeatable {
        if (targetPos == null || dir == null) {
            return@repeatable
        }

        // some blocks only break when holding a certain tool
        val oldSlot = player.inventory.selectedSlot
        val state = world.getBlockState(targetPos)
        var shouldSwitch = switch && state.isToolRequired
        if (shouldSwitch && ModuleAutoTool.enabled) {
            ModuleAutoTool.switchToBreakBlock(targetPos!!)
            shouldSwitch = false
        } else if (shouldSwitch) {
            val slot = findHotbarSlot { stack -> stack.isSuitableFor(state) } ?: -1
            if (slot != -1 && slot != oldSlot) {
                network.sendPacket(UpdateSelectedSlotC2SPacket(slot))
            } else {
                shouldSwitch = false
            }
        }

        rotateToTargetBlock(state)

        // Alright, for some reason when we spam STOP_DESTROY_BLOCK
        // server accepts us to destroy the same block instantly over and over.
        network.sendPacket(PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, targetPos, dir))

        if (shouldSwitch) {
            network.sendPacket(UpdateSelectedSlotC2SPacket(oldSlot))
        }
    }

    // some anti-cheats have interact checks
    private fun rotateToTargetBlock(state: BlockState) {
        if (!rotate) {
            return
        }

        val raytrace = raytraceBlock(
            player.eyes,
            targetPos!!,
            state,
            range = 25.0,
            wallsRange = 25.0
        )

        if (raytrace == null) {
            return // still send the packet, so we don't lose the block
        }

        val (rotation, _) = raytrace
        RotationManager.aimAt(
            rotation,
            considerInventory = !ignoreOpenInventory,
            configurable = rotationsConfigurable,
            Priority.IMPORTANT_FOR_USAGE_2,
            ModuleCivBreak
        )
    }

    @Suppress("unused")
    private val mouseButtonHandler = handler<MouseButtonEvent> { event ->
        mc.currentScreen?.let { return@handler }

        val isLeftClick = event.button == 0
        // without adding a little delay before being able to unselect / select again, selecting would be impossible
        val hasTimePassed = chronometer.hasElapsed(200)
        val hitResult = mc.crosshairTarget
        if (!isLeftClick || !hasTimePassed || hitResult == null || hitResult !is BlockHitResult) {
            return@handler
        }

        // mining unbreakable (-1) or instant breaking (0) blocks with this doesn't make sense
        val shouldTargetBlock = world.getBlockState(hitResult.blockPos).getHardness(world, hitResult.blockPos) > 0f
        // stop when the block is clicked again
        val isCancelledByUser = hitResult.blockPos.equals(targetPos)

        if (shouldTargetBlock && world.worldBorder.contains(hitResult.blockPos) && !isCancelledByUser) {
            targetPos = hitResult.blockPos
            dir = hitResult.side
        } else {
            targetPos = null
            dir = null
        }

        chronometer.reset()
    }

}
