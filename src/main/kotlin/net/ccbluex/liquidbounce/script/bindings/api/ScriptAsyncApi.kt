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
package net.ccbluex.liquidbounce.script.bindings.api

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import org.graalvm.polyglot.Value

@Suppress("unused")
object ScriptAsyncApi : MinecraftShortcuts {

    private val MainDispatcher = mc.asCoroutineDispatcher()

    private val scope = CoroutineScope(MainDispatcher + SupervisorJob())

    private var idCounter = 0

    private val timeoutMap = Int2ObjectOpenHashMap<Job>()
    private val intervalMap = Int2ObjectOpenHashMap<Job>()

    @JvmName("setTimeout")
    @Synchronized
    fun setTimeout(callback: Value, delay: Long, vararg arguments: Any?): Int {
        val id = ++idCounter
        timeoutMap.put(id, scope.launch {
            delay(delay)
            callback.executeVoid(*arguments)
        })
        return id
    }

    @JvmName("clearTimeout")
    @Synchronized
    fun clearTimeout(id: Int) {
        timeoutMap[id]?.cancel()
        timeoutMap.remove(id)
    }

    @JvmName("setInterval")
    @Synchronized
    fun setInterval(callback: Value, delay: Long, vararg arguments: Any?): Int {
        val id = ++idCounter
        timeoutMap.put(id, scope.launch {
            while (isActive) {
                delay(delay)
                callback.executeVoid(*arguments)
            }
        })
        return id
    }

    @JvmName("clearInterval")
    @Synchronized
    fun clearInterval(id: Int) {
        intervalMap[id]?.cancel()
        intervalMap.remove(id)
    }

}
