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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place

import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.CrystalAuraDamageOptions
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

class PlacementPositionCandidate(
    val pos: BlockPos, // the block the crystal should be placed on
    val notBlockedByCrystal: Boolean,
    val requiresBasePlace: Boolean
) {

    /**
     * The damage a crystal at the specific position would deal.
     */
    var explosionDamage: Float? = null

    init {
        calculate()
    }

    /**
     * Evaluates the explosion damage to the target, sets it to `null` if the position is invalid.
     */
    fun calculate() {
        val damageSourceLoc = Vec3d.of(pos).add(0.5, 1.0, 0.5)
        explosionDamage = CrystalAuraDamageOptions.approximateExplosionDamage(
            damageSourceLoc,
            if (requiresBasePlace) {
                CrystalAuraDamageOptions.RequestingSubmodule.BASE_PLACE
            } else {
                CrystalAuraDamageOptions.RequestingSubmodule.PLACE
            }
        )
    }

    fun isNotInvalid() = explosionDamage != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlacementPositionCandidate

        if (pos != other.pos) return false
        if (requiresBasePlace != other.requiresBasePlace) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pos.hashCode()
        result = 31 * result + requiresBasePlace.hashCode()
        return result
    }

}
