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
package net.ccbluex.liquidbounce.utils.kotlin

import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.client.MinecraftClient
import java.util.PriorityQueue

class RequestHandler<T> {
    @Volatile
    private var currentTick = 0

    private val activeRequests = PriorityQueue<Request<T>>(11, compareBy { -it.priority })

    private val lock = ReentrantLock()

    fun tick(deltaTime: Int = 1) {
        currentTick += deltaTime
    }

    fun request(request: Request<T>) {
        lock.withLock {
            // we remove all requests provided by module on new request
            activeRequests.removeAll { it.provider == request.provider }
            request.expiresIn += currentTick
            activeRequests.add(request)
        }
    }

    fun getActiveRequestValue(): T? {
        lock.withLock {
            var top = activeRequests.peek() ?: return null

            if (MinecraftClient.getInstance()?.isOnThread != false) {
                // we remove all outdated requests here
                while (top.expiresIn <= currentTick || !top.provider.running) {
                    activeRequests.remove()
                    top = activeRequests.peek() ?: return null
                }
            }

            return top.value
        }
    }

    /**
     * A requested state of the system.
     *
     * Note: A request is deleted when its corresponding module is disabled.
     *
     * @param expiresIn in how many ticks units should this request expire?
     * @param priority higher = higher priority
     * @param provider module which requested value
     */
    class Request<T>(
        var expiresIn: Int, val priority: Int, val provider: ClientModule, val value: T
    )
}
