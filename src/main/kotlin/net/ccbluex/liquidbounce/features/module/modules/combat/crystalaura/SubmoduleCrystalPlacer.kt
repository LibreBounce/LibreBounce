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
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.aiming.*
import net.ccbluex.liquidbounce.utils.block.*
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
import kotlin.math.max

object SubmoduleCrystalPlacer : ToggleableConfigurable(ModuleCrystalAura, "Place", true) {

    private val swingMode by enumChoice("Swing", SwingMode.DO_NOT_HIDE)
    private val switchMode by enumChoice("Switch", SwitchMode.SILENT)
    val oldVersion by boolean("1_12_2", false)
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
                swingMode,
                switchMode
            )

            SubmoduleIdPredict.run(targetPos)

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

    @Suppress("ComplexCondition", "LongMethod", "CognitiveComplexMethod")
    private fun updateTarget() {
        // Reset current target
        previousTarget = placementTarget
        placementTarget = null

        val playerEyePos = player.eyePos
        val range = range.toDouble()
        val wallsRange = wallsRange.toDouble()

        val target = ModuleCrystalAura.currentTarget ?: return
        val maxY = target.boundingBox.maxY

        val positions = mutableListOf<PlacementPositionCandidate>()

        val box = if (oldVersion) FULL_BOX.withMaxX(2.0) else FULL_BOX

        val basePlace = SubmoduleBasePlace.shouldBasePlaceRun()
        val currentBasePlaceTarget = if (basePlace) null else SubmoduleBasePlace.currentTarget
        val basePlaceLayers = if (basePlace) SubmoduleBasePlace.getBasePlaceLayers(target.y) else IntOpenHashSet()

        val playerPos = player.blockPos
        val pos = BlockPos.Mutable()
        sphere.forEach {
            pos.set(playerPos).move(it)
            val state = pos.getState()!!
            val canSeeUpperBlockSide = !onlyAbove || canSeeUpperBlockSide(playerEyePos, pos, range, wallsRange)
            val canPlace = state.block == Blocks.OBSIDIAN || state.block == Blocks.BEDROCK

            if (pos.up().getState()!!.isAir &&
                (!oldVersion || pos.up(2).getState()!!.isAir) &&
                canSeeUpperBlockSide &&
                pos.y.toDouble() + 1.0 < maxY &&
                (canPlace || SubmoduleBasePlace.canBasePlace(basePlace, pos, basePlaceLayers, state))
            ) {
                val up = pos.up()
                if (PredictFeature.willBeBlocked(
                    box.offset(up.x.toDouble(), up.y.toDouble(), up.z.toDouble()),
                    target,
                    !canPlace
                )) {
                    return@forEach
                }

                val blocked = up.isBlockedByEntitiesReturnCrystal(
                    box = box
                )

                val crystal = blocked.value() != null
                if (!blocked.keyBoolean() || crystal) {
                    positions.add(PlacementPositionCandidate(pos.toImmutable(), !crystal, !canPlace))
                }
            }
        }

        val finalPositions = positions.filter(PlacementPositionCandidate::isNotInvalid)
        var bestTarget = finalPositions.maxByOrNull { it.explosionDamage!! } ?: return

        if (bestTarget.requiresBasePlace) {
            finalPositions.filterNot { it.requiresBasePlace }.maxByOrNull { it.explosionDamage!! }?.let {
                if (it.explosionDamage!! - bestTarget.explosionDamage!! >= SubmoduleBasePlace.minAdvantage) {
                    bestTarget = it
                }
            }
        }

        currentBasePlaceTarget?.let {
            it.calculate()
            if (it.isNotInvalid() &&
                it.explosionDamage!! - bestTarget.explosionDamage!! >= SubmoduleBasePlace.minAdvantage) {
                bestTarget = it
            }
        }

        if (bestTarget.requiresBasePlace && bestTarget != currentBasePlaceTarget) {
            SubmoduleBasePlace.currentTarget = bestTarget
        } else if (bestTarget != currentBasePlaceTarget) {
            SubmoduleBasePlace.currentTarget = null
        }

        if (bestTarget.notBlockedByCrystal && !bestTarget.requiresBasePlace) {
            placementTarget = bestTarget.pos
        }
    }

}
