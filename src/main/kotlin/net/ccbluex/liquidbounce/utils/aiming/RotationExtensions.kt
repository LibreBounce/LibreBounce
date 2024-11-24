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
package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.utils.aiming.RotationManager.angleDifference
import net.minecraft.client.network.ClientPlayerEntity

fun ClientPlayerEntity.setRotation(rotation: Rotation) {
    rotation.normalize().let { normalizedRotation ->
        prevPitch = pitch
        prevYaw = yaw
        renderYaw = yaw
        lastRenderYaw = yaw

        yaw = normalizedRotation.yaw
        pitch = normalizedRotation.pitch
    }
}

fun ClientPlayerEntity.withFixedYaw(rotation: Rotation) = rotation.yaw + angleDifference(yaw, rotation.yaw)
