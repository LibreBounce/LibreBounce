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
package net.ccbluex.liquidbounce.features.module.modules.world.traps

import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.world.traps.traps.IgnitionTrapPlanner
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raycast
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.util.Hand

/**
 * Ignite module
 *
 * Automatically sets targets around you on fire.
 */
object ModuleAutoTrap : ClientModule("AutoTrap", Category.WORLD, aliases = arrayOf("Ignite")) {

    val range by floatRange("Range", 3.0f..4.5f, 2f..6f)
    private val delay by int("Delay", 20, 0..400, "ticks")
    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    private val ignitionTrapPlanner = IgnitionTrapPlanner(this)

    private val rotationsConfigurable = tree(RotationsConfigurable(this))

    private var currentPlan: BlockChangeIntent<*>? = null

    private var timeout = false

    override fun enable() {
        timeout = false
    }

    override fun disable() {
        timeout = false
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        if (timeout) {
            return@handler
        }

        this.currentPlan = ignitionTrapPlanner.plan()

        this.currentPlan?.let {
            RotationManager.aimAt(
                (it.blockChangeInfo as BlockChangeInfo.PlaceBlock).blockPlacementTarget.rotation,
                considerInventory = !ignoreOpenInventory,
                configurable = rotationsConfigurable,
                Priority.IMPORTANT_FOR_PLAYER_LIFE,
                this
            )
        }
    }

    @Suppress("unused")
    private val placementHandler = tickHandler {
        val plan = currentPlan ?: return@tickHandler
        val raycast = raycast() ?: return@tickHandler

        if (!plan.validate(raycast)) {
            return@tickHandler
        }

        CombatManager.pauseCombatForAtLeast(1)

        SilentHotbar.selectSlotSilently(this, plan.slot.hotbarSlotForServer, 1)

        doPlacement(raycast, Hand.MAIN_HAND)

        timeout = true

        plan.onIntentFullfilled()

        waitTicks(delay)

        timeout = false
    }
}
