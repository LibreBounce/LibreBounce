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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.QuickImports
import net.ccbluex.liquidbounce.utils.client.RestrictedSingleUseAction
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

abstract class RotationMode(
    name: String,
    private val configurable: ChoiceConfigurable<RotationMode>,
    val module: Module,
) : Choice(name), QuickImports {

    /**
     * Already sends the packet on post-move.
     * This might get us a little advantage because the packets are added a little bit earlier to the server tick queue.
     *
     * The downside is that it is not legit and will flag post-rotation checks on some anti-cheats.
     */
    val postMove by boolean("PostMove", false)

    abstract fun rotate(rotation: Rotation, isFinished: () -> Boolean, onFinished: () -> Unit)

    override val parent: ChoiceConfigurable<*>
        get() = configurable

}

class NormalRotationMode(
    configurable: ChoiceConfigurable<RotationMode>,
    module: Module,
    val priority: Priority = Priority.IMPORTANT_FOR_USAGE_2
) : RotationMode("Normal", configurable, module) {

    val rotations = tree(RotationsConfigurable(this))
    val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    override fun rotate(rotation: Rotation, isFinished: () -> Boolean, onFinished: () -> Unit) {
        RotationManager.aimAt(
            rotation,
            considerInventory = !ignoreOpenInventory,
            configurable = rotations,
            provider = module,
            priority = priority,
            whenReached = RestrictedSingleUseAction(canExecute = isFinished, action = {
                PostRotationExecutor.addTask(module, postMove, task = onFinished, priority = true)
            })
        )
    }

}

class NoRotationMode(configurable: ChoiceConfigurable<RotationMode>, module: Module)
    : RotationMode("None", configurable, module) {

    val send by boolean("SendRotationPacket", false)

    override fun rotate(rotation: Rotation, isFinished: () -> Boolean, onFinished: () -> Unit) {
        PostRotationExecutor.addTask(module, postMove, task = {
            if (send) {
                val fixedRotation = rotation.fixedSensitivity()
                network.connection!!.send(
                    PlayerMoveC2SPacket.LookAndOnGround(fixedRotation.yaw, fixedRotation.pitch, player.isOnGround),
                    null
                )
            }

            onFinished()
        })
    }

}


