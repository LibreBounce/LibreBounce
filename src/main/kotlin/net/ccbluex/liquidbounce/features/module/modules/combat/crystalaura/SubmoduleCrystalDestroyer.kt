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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

import it.unimi.dsi.fastutil.objects.ObjectFloatImmutablePair
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.canSeeBox
import net.ccbluex.liquidbounce.utils.aiming.facingEnemy
import net.ccbluex.liquidbounce.utils.aiming.raytraceBox
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.combat.attack
import net.ccbluex.liquidbounce.utils.combat.getEntitiesBoxInRange
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.entity.decoration.EndCrystalEntity
import kotlin.math.max

object SubmoduleCrystalDestroyer : ToggleableConfigurable(ModuleCrystalAura, "Destroy", true) {

    val swing by boolean("Swing", true)
    private val delay by int("Delay", 0, 0..1000, "ms")
    val range by float("Range", 4.5F, 1.0F..5.0F)
    val wallsRange by float("WallsRange", 4.5F, 1.0F..5.0F)

    var postAttackHandlers = arrayOf(CrystalAuraSpeedDebugger, SubmoduleSetDead.CrystalTracker)
    val chronometer = Chronometer()
    private var currentTarget: EndCrystalEntity? = null

    fun tick(providedCrystal: EndCrystalEntity? = null) {
        if (!enabled || !chronometer.hasAtLeastElapsed(delay.toLong())) {
            return
        }

        val range = range.toDouble()
        val wallsRange = wallsRange.toDouble()

        providedCrystal?.let { crystal ->
            // just check if it works, not if it's the best
            validateAndUpdateTarget(crystal)
        } ?: run {
            updateTarget()
        }

        val target = currentTarget ?: return

        // find the best spot (and skip if no spot was found)
        val (rotation, _) =
            raytraceBox(
                player.eyePos,
                target.boundingBox,
                range = range,
                wallsRange = wallsRange,
            ) ?: return

        ModuleCrystalAura.rotationMode.activeChoice.rotate(rotation, isFinished = {
            facingEnemy(
                toEntity = target,
                rotation = RotationManager.serverRotation,
                range = range,
                wallsRange = wallsRange
            )
        }, onFinished = {
            if (!chronometer.hasAtLeastElapsed(delay.toLong())) {
                return@rotate
            }

            val target1 = currentTarget ?: return@rotate

            target1.attack(swing)
            postAttackHandlers.forEach { it.attacked(target1.id) }
            chronometer.reset()
        })
    }

    private fun updateTarget() {
        val range = range.toDouble()
        val wallsRange = wallsRange.toDouble()
        val maxRange = max(wallsRange, range)
        currentTarget =
            world.getEntitiesBoxInRange(player.getCameraPosVec(1.0F), maxRange) { it is EndCrystalEntity }
                .mapNotNull {
                    if (!canSeeBox(
                            player.eyePos,
                            it.boundingBox,
                            range = range,
                            wallsRange = wallsRange,
                        )
                    ) {
                        return@mapNotNull null
                    }

                    val damage = CrystalAuraDamageOptions.approximateExplosionDamage(
                        it.pos,
                        CrystalAuraDamageOptions.RequestingSubmodule.DESTROY
                    ) ?: return@mapNotNull null

                    ObjectFloatImmutablePair(it as EndCrystalEntity, damage)
                }
                .maxByOrNull { it.secondFloat() }?.first()
    }

    private fun validateAndUpdateTarget(entity: EndCrystalEntity) {
        val range = range.toDouble()
        val wallsRange = wallsRange.toDouble()
        val maxRange = max(wallsRange, range) + entity.boundingBox.maxX - entity.boundingBox.minX
        currentTarget = null
        if (player.eyePos.squaredDistanceTo(entity.pos) > maxRange.sq()) {
            return
        }

        if (!canSeeBox(
                player.eyePos,
                entity.boundingBox,
                range = range,
                wallsRange = wallsRange,
            )
        ) {
            return
        }

        CrystalAuraDamageOptions.approximateExplosionDamage(
            entity.pos,
            CrystalAuraDamageOptions.RequestingSubmodule.DESTROY
        ) ?: return

        currentTarget = entity
    }

    fun getMaxRange() = max(range, wallsRange)

}
