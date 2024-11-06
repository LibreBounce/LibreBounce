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

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention

/**
 * Executes code right after the client sent the normal movement packet.
 */
object PostRotationExecutor : Listenable {

    /**
     * This should be used by actions that depend on the rotation sent in the tick movement packet.
     */
    private var priorityAction: Pair<Module, () -> Unit>? = null
        set(value) {
            if (field != null) {
                // normally this should not happen!
                print("NOT NULL! ${value?.first?.name} tried to replace ${field?.first?.name}")
            }
            field = value
        }

    private var priorityActionPostMove = false

    /**
     * All other actions that should be executed on post-move.
     */
    private val postMoveTasks = ArrayDeque<Pair<Module, () -> Unit>>()

    /**
     * All other actions that should be executed on tick.
     */
    private val normalTasks = ArrayDeque<Pair<Module, () -> Unit>>()

    @Suppress("unused")
    val worldChangeHandler = handler<WorldChangeEvent> {
        postMoveTasks.clear()
        normalTasks.clear()
    }

    /**
     * Executes the currently waiting actions.
     *
     * Has [EventPriorityConvention.FIRST_PRIORITY] to run before any other module can send packets.
     */
    @Suppress("unused")
    val networkMoveHandler = handler<PlayerNetworkMovementTickEvent>(priority = EventPriorityConvention.FIRST_PRIORITY) {
        if (it.state != EventState.POST) {
            return@handler
        }

        // if the priority action doesn't run on post-move, no other action can
        if (!priorityActionPostMove && priorityAction != null) {
           return@handler
        }

        priorityAction?.let { action ->
            if (action.first.enabled) {
                action.second.invoke()
            }
        }

        priorityAction = null

        // execute all other actions
        while (postMoveTasks.isNotEmpty()) {
            val next = postMoveTasks.removeFirst()
            if (next.first.enabled) {
                next.second.invoke()
            }
        }
    }

    /**
     * Executes the currently waiting actions.
     *
     * Has [EventPriorityConvention.FIRST_PRIORITY] to run before any other module can send packets.
     */
    @Suppress("unused")
    val tickHandler = handler<GameTickEvent>(priority = EventPriorityConvention.FIRST_PRIORITY) {
        if (!priorityActionPostMove) {
            // execute the priority action
            priorityAction?.let { action ->
                if (action.first.enabled) {
                    action.second.invoke()
                }

                // if we reach this point, the post-move queue has not been processed yet because it was waiting for
                // the priority action
                while (postMoveTasks.isNotEmpty()) {
                    val next = postMoveTasks.removeFirst()
                    if (next.first.enabled) {
                        next.second.invoke()
                    }
                }
            }

            priorityAction = null
        }

        // execute all other actions
        while (normalTasks.isNotEmpty()) {
            val next = normalTasks.removeFirst()
            if (next.first.enabled) {
                next.second.invoke()
            }
        }
    }

    fun addTask(module: Module, postMove: Boolean, task: () -> Unit, priority: Boolean = false) {
        if (priority) {
            priorityAction = module to task
            priorityActionPostMove = postMove
        } else if (postMove) {
            postMoveTasks.add(module to task)
        } else {
            normalTasks.add(module to task)
        }
    }

}
