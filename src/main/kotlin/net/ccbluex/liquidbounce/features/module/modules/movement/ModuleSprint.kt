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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldSprintControlFeature
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput

/**
 * Sprint module
 *
 * Sprints automatically.
 */

object ModuleSprint : ClientModule("Sprint", Category.MOVEMENT) {

    private enum class SprintMode(override val choiceName: String) : NamedChoice {
        LEGIT("Legit"),
        OMNIDIRECTIONAL("Omnidirectional"),
        OMNIROTATIONAL("Omnirotational"),
    }

    private val sprintMode by enumChoice("Mode", SprintMode.LEGIT)

    private val ignoreBlindness by boolean("IgnoreBlindness", false)
    private val ignoreHunger by boolean("IgnoreHunger", false)
    private val ignoreCollision by boolean("IgnoreCollision", false)

    val shouldSprintOmnidirectional: Boolean
        get() = running && sprintMode == SprintMode.OMNIDIRECTIONAL ||
            ScaffoldSprintControlFeature.allowOmnidirectionalSprint

    val shouldIgnoreBlindness
        get() = running && ignoreBlindness

    val shouldIgnoreHunger
        get() = running && ignoreHunger

    val shouldIgnoreCollision
        get() = running && ignoreCollision

    @Suppress("unused")
    private val sprintHandler = handler<SprintEvent>(
        priority = EventPriorityConvention.FIRST_PRIORITY
    ) { event ->
        if (!event.directionalInput.isMoving) {
            return@handler
        }

        if (event.source == SprintEvent.Source.MOVEMENT_TICK || event.source == SprintEvent.Source.INPUT) {
            event.sprint = true
        }
    }

    // DO NOT USE TREE TO MAKE SURE THAT THE ROTATIONS ARE NOT CHANGED
    private val rotationsConfigurable = RotationsConfigurable(this)

    @Suppress("unused")
    private val omniRotationalHandler = handler<GameTickEvent> {
        // Check if omnirotational sprint is enabled
        if (sprintMode != SprintMode.OMNIROTATIONAL) {
            return@handler
        }

        val yaw = getMovementDirectionOfInput(player.yaw, DirectionalInput(player.input))

        // todo: unhook pitch - AimPlan needs support for only yaw or pitch operation
        val rotation = Rotation(yaw, player.pitch)

        RotationManager.aimAt(rotationsConfigurable.toAimPlan(rotation), Priority.NOT_IMPORTANT,
            this@ModuleSprint)
    }
}
