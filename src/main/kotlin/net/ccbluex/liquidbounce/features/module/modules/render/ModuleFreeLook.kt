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

package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.minecraft.client.option.Perspective.THIRD_PERSON_BACK

object ModuleFreeLook : Module(
    "FreeLook", Category.RENDER, disableOnQuit = true, bindAction = InputBind.BindAction.HOLD
) {

    private val noPitchLimit by boolean("NoPitchLimit", true)

    var cameraYaw = 0f
    var cameraPitch = 0f

    override fun enable() {
        cameraYaw = player.yaw
        cameraPitch = player.pitch
        mc.options.perspective = THIRD_PERSON_BACK
    }

    @Suppress("unused")
    private val inputHandler = handler<InputHandleEvent> {
        if (mc.options.togglePerspectiveKey.isPressed) {
            enabled = false
        }
    }

    @Suppress("unused")
    private val handlePerspective = handler<PerspectiveEvent> { event ->
        event.perspective = THIRD_PERSON_BACK
    }

    @Suppress("unused")
    private val mouseRotationInputHandler = handler<MouseRotationEvent> { event ->
        cameraYaw += event.cursorDeltaX.toFloat() * 0.15f
        cameraPitch += event.cursorDeltaY.toFloat() * 0.15f

        if (!noPitchLimit) {
            cameraPitch = cameraPitch.coerceIn(-90f..90f)
        }

        event.cancelEvent()
    }
}
