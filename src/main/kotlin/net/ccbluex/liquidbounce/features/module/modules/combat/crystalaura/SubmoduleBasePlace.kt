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
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.utils.block.placer.BlockPlacer
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.item.findHotbarItemSlot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.item.Items
import net.minecraft.util.math.BlockPos
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

// TODO enemy predict to get positions that damage a long time (optimal ticks = cps / 20)
// TODO placer -> support -> exclude above blocks to not block crystals
/**
 * Tries to build improved placement spots.
 */
object SubmoduleBasePlace : ToggleableConfigurable(ModuleCrystalAura, "BasePlace", true) {

    /**
     * How long to wait before starting a new calculation.
     */
    private val delay by int("CalcDelay", 80, 0..1000, "ms") // TODO separate place delay

    /**
     * After how many ms the placer get cleared.
     */
    private val timeOut by int("TimeOut", 160, 0..5000, "ms")

    /**
     * Only places bellow the enemy when this is true.
     */
    private val platformOnly by boolean("PlatformOnly", false)

    /**
     * Excludes terrain for support placements. This can make the ca very inefficient in scuffed landscapes.
     *
     * Only has an effect if [ModuleCrystalAura.DamageOptions.terrain] is enabled.
     */
    val terrain by boolean("Terrain", true)

    /**
     * Makes sure we don't run into the placement. This does not mean the damage will be predicted at the simulated
     * position.
     *
     * A value of 0 indicates it should not be checked. // TODO higher default and down error or anti self platform (pos y >= self y)?
     */
    private val simulateMovement by int("SimulateMovement", 3, 0..20, "ticks")

    val minAdvantage by float("MinAdvantage", 2.0f, 0.1f..10f)

    private val placer = tree(BlockPlacer("Placing", ModuleCrystalAura, Priority.IMPORTANT_FOR_USAGE_2, slotFinder = { _ ->
        findHotbarItemSlot(Items.OBSIDIAN) ?: findHotbarItemSlot(Items.BEDROCK)
    }))

    var currentTarget: PlacementPositionCandidate? = null
        set(value) {
            field = value
            value?.let {
                placer.update(setOf(it.pos))
                trying.reset()
            } ?: run { placer.clear() }
        }

    private val calculations = Chronometer()
    private val trying = Chronometer()

    val repeatable = repeatable {
        if (currentTarget != null && trying.hasElapsed(timeOut.toLong())) {
            placer.clear()
            currentTarget = null
        }
    }

    override fun disable() {
        placer.disable()
        currentTarget = null
    }

    /**
     * Returns whether support should be calculated.
     */
    fun shouldSupportRun(): Boolean {
        if (!enabled) {
            return false
        }

        if (calculations.hasAtLeastElapsed(delay.toLong())) {
            calculations.reset()
            return true
        }

        return false
    }

    /**
     * Returns a set of y levels the support blocks can be placed in.
     */
    fun getSupportLayers(targetY: Double): IntOpenHashSet {
        var down = 3
        var maxY = if (targetY % 1 > 0.2) {
            down++
            ceil(targetY).toInt()
        } else {
            floor(targetY).toInt()
        }

        if (platformOnly) {
            maxY--
            down--
        }

        val result = IntOpenHashSet()
        repeat(down) {
            result.add(maxY)
            maxY--
        }

        return result
    }

    fun playerWillNotRunIn(pos: BlockPos): Boolean {
        if (simulateMovement == 0) {
            return true
        }

        val snapShots = PlayerSimulationCache
            .getSimulationForLocalPlayer()
            .getSnapshotsBetween(1..simulateMovement)

        // check if the pos will intersect at any expected position
        // 0.1 as error offset // TODO offset might be too low?
        return snapShots.any { FULL_BOX.offset(pos).intersects(
            pos.x.toDouble() - 0.1,
            pos.y.toDouble(),
            pos.z.toDouble() - 0.1,
            pos.x.toDouble() + 1.1,
            pos.y.toDouble() + 1.9,
            pos.z.toDouble() + 1.1
        ) }
    }

    fun getMaxRange(): Float = max(placer.range, placer.wallRange)

}
