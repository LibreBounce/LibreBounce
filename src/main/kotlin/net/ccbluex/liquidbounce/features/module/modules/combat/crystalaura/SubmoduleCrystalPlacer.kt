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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

import it.unimi.dsi.fastutil.objects.ObjectFloatImmutablePair
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.aiming.*
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.block.getSortedSphere
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.isBlockedByEntitiesReturnCrystal
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.clickBlockWithSlot
import net.ccbluex.liquidbounce.utils.inventory.OFFHAND_SLOT
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
import net.minecraft.block.Blocks
import net.minecraft.item.Items
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import kotlin.math.max

object SubmoduleCrystalPlacer : ToggleableConfigurable(ModuleCrystalAura, "Place", true) {

    private var swingMode by enumChoice("Swing", SwingMode.DO_NOT_HIDE)
    private val delay by int("Delay", 0, 0..1000, "ms")
    private val range by float("Range", 4.5F, 1.0F..5.0F).onChanged { updateSphere() }
    private val wallsRange by float("WallsRange", 4.5F, 1.0F..5.0F).onChanged {
        updateSphere()
    }

    private val onlyAbove by boolean("OnlyAbove", false)

    val placementRenderer = tree(PlacementRenderer( // TODO slide
        "TargetRendering",
        true,
        ModuleCrystalAura,
        clump = false,
        defaultColor = Color4b.WHITE.alpha(90)
    ))

    private val chronometer = Chronometer()
    private var sphere: Array<BlockPos> = BlockPos.ORIGIN.getSortedSphere(4.5f)
    private var placementTarget: BlockPos? = null
    private var previousTarget: BlockPos? = null
    private var blockHitResult: BlockHitResult? = null

    private fun updateSphere() {
        sphere = BlockPos.ORIGIN.getSortedSphere(max(range, wallsRange))
    }

    @Suppress("LongMethod", "CognitiveComplexMethod")
    fun tick() {
        if (!enabled || !chronometer.hasAtLeastElapsed(delay.toLong())) {
            return
        }

        getSlot() ?: return

        updateTarget()
        if (placementTarget != previousTarget) {
            previousTarget?.let { placementRenderer.removeBlock(it) }
        }

        val targetPos = placementTarget ?: return

        var side = Direction.UP
        val rotation = if (onlyAbove) {
            raytraceUpperBlockSide(
                player.eyePos,
                range.toDouble(),
                wallsRange.toDouble(),
                targetPos,
            )
        } else {
            val data = findClosestPointOnBlock(
                player.eyePos,
                range.toDouble(),
                wallsRange.toDouble(),
                targetPos,
            ) ?: return
            side = data.second

            data.first
        } ?: return

        if (ModuleCrystalAura.rotationMode.activeChoice is NoRotationMode) {
            blockHitResult = raytraceBlock(
                max(range, wallsRange).toDouble(),
                rotation.rotation,
                targetPos,
                targetPos.getState()!!
            ) ?: return
        }

        if (placementTarget != previousTarget) {
            placementTarget?.let { placementRenderer.addBlock(it) }
        }

        ModuleCrystalAura.rotationMode.activeChoice.rotate(rotation.rotation, isFinished = {
            blockHitResult = raytraceBlock(
                max(range, wallsRange).toDouble(),
                RotationManager.serverRotation,
                targetPos,
                targetPos.getState()!!
            ) ?: return@rotate false

            return@rotate blockHitResult!!.type == HitResult.Type.BLOCK && blockHitResult!!.blockPos == targetPos
        }, onFinished = {
            if (!chronometer.hasAtLeastElapsed(delay.toLong())) {
                return@rotate
            }

            clickBlockWithSlot(
                player,
                blockHitResult?.withSide(side) ?: return@rotate,
                getSlot() ?: return@rotate,
                swingMode
            )

            chronometer.reset()
        })
    }

    private fun getSlot(): Int? {
        return if (OFFHAND_SLOT.itemStack.item == Items.END_CRYSTAL) {
            OFFHAND_SLOT.hotbarSlotForServer
        } else {
            findHotbarSlot(Items.END_CRYSTAL)
        }
    }

    @Suppress("ComplexCondition")
    private fun updateTarget() {
        // Reset current target
        previousTarget = placementTarget
        placementTarget = null

        val playerEyePos = player.eyePos
        val range = range.toDouble()
        val wallsRange = wallsRange.toDouble()

        val target = ModuleCrystalAura.currentTarget ?: return
        val maxX = target.boundingBox.maxX

        val positions = mutableListOf<ObjectObjectImmutablePair<BlockPos, Boolean>>()

        val playerPos = player.blockPos
        val pos = BlockPos.Mutable()
        sphere.forEach {
            pos.set(playerPos).move(it)
            val state = pos.getState()!!
            val canSeeUpperBlockSide = !onlyAbove || canSeeUpperBlockSide(playerEyePos, pos, range, wallsRange)
            if ((state.block == Blocks.OBSIDIAN || state.block == Blocks.BEDROCK) &&
                pos.up().getState()!!.isAir &&
                canSeeUpperBlockSide &&
                pos.x.toDouble() + 1.0 < maxX
            ) {
                val blocked = pos.up().isBlockedByEntitiesReturnCrystal()

                val crystal = blocked.value() != null
                if (!blocked.keyBoolean() || crystal) {
                    positions.add(ObjectObjectImmutablePair(pos.toImmutable(), !crystal))
                }
            }
        }

        val bestTarget =
            positions
                .mapNotNull {
                    val damageSourceLoc = Vec3d.of(it.left()).add(0.5, 1.0, 0.5)
                    val explosionDamage = ModuleCrystalAura.approximateExplosionDamage(damageSourceLoc)
                        ?: return@mapNotNull null
                    ObjectFloatImmutablePair(it, explosionDamage)
                }
                .maxByOrNull { it.secondFloat() }


        if (bestTarget != null && bestTarget.left().second()) {
            placementTarget = bestTarget.first().first()
        }
    }

}
