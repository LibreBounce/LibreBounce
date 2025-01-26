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
import java.util.function.IntConsumer

@Suppress("SpreadOperator", "unused")
object ScriptAsyncApi : MinecraftShortcuts {

    private val MainDispatcher = mc.asCoroutineDispatcher()

    private val scope = CoroutineScope(MainDispatcher + SupervisorJob())

    private var idCounter = 0

    private val timeoutMap = Int2ObjectOpenHashMap<Job>()
    private val intervalMap = Int2ObjectOpenHashMap<Job>()

    @FunctionalInterface // this is needed for Polyglot script functions
    internal fun interface SetFunction {
        operator fun invoke(callback: Value, delay: Long, vararg arguments: Any?): Int
    }

    internal val setTimeout = SetFunction { callback, delay, arguments ->
        val id = ++idCounter
        timeoutMap.put(id, scope.launch {
            delay(delay)
            callback.executeVoid(*arguments)
            timeoutMap.remove(id)
        })
        id
    }

    internal val clearTimeout = IntConsumer { id ->
        timeoutMap.remove(id)?.cancel()
    }

    internal val setInterval = SetFunction { callback, delay, arguments ->
        val id = ++idCounter
        intervalMap.put(id, scope.launch {
            while (isActive) {
                delay(delay)
                callback.executeVoid(*arguments)
                intervalMap.remove(id)
            }
        })
        id
    }

    internal val clearInterval = IntConsumer { id ->
        intervalMap.remove(id)?.cancel()
    }

}
