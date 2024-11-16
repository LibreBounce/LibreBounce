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

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Counts how many crystals the crystal aura places.
 * Cps stands for crystals per second.
 */
object CrystalAuraSpeedDebugger : CrystalPostAttackTracker() {

    private var cps = ConcurrentLinkedDeque<Long>()

    @Suppress("unused")
    val repeatable1 = repeatable {
        val currentTime = System.currentTimeMillis()
        val cpsTime = currentTime - 1000L
        while (cps.isNotEmpty()) {
            if (cps.first >= cpsTime) {
                break
            }

            cps.removeFirst()
        }

        ModuleDebug.debugParameter(ModuleCrystalAura, "CPS", cps.size)
    }

    override fun confirmed(id: Int) {
        cps.add(System.currentTimeMillis())
    }

    override fun cleared() {
        attackedIds.clear()
    }

    override fun handleEvents() = ModuleCrystalAura.handleEvents() && ModuleDebug.enabled

}
