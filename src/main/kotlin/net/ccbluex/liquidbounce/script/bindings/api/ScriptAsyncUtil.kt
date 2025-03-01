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

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.script.ScriptApiRequired
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.util.function.BooleanSupplier

/**
 * @author MukjepScarlet
 */
class ScriptAsyncUtil(
    private val jsPromiseConstructor: Value
) {

    companion object TickScheduler : EventListener {

        private val currentTickTasks = arrayListOf<BooleanSupplier>()
        private val nextTickTasks = arrayListOf<BooleanSupplier>()

        @Suppress("unused")
        private val tickHandler = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
            currentTickTasks.removeIf { it.asBoolean }
            currentTickTasks += nextTickTasks
            nextTickTasks.clear()
        }

        private fun schedule(breakLoop: BooleanSupplier) {
            mc.execute { nextTickTasks += breakLoop }
        }
    }

    private val defaultPromise: Value = jsPromiseConstructor.invokeMember("resolve", 0);

    /**
     * Example: `await ticks(10)`
     *
     * @return `Promise<number>`
     */
    @ScriptApiRequired
    fun ticks(n: Int): Value {
        if (n == 0) {
            return defaultPromise
        }

        var remains = n
        return until { --remains == 0 }
    }

    /**
     * Example: `await seconds(1)`
     *
     * @return `Promise<number>`
     */
    @ScriptApiRequired
    fun seconds(n: Int): Value = ticks(n * 20)

    /**
     * Example: `const duration = await until(() => mc.player.isOnGround())`
     *
     * @return `Promise<number>`
     */
    @ScriptApiRequired
    fun until(condition: BooleanSupplier): Value = jsPromiseConstructor.newInstance(
        ProxyExecutable { (onResolve, onReject) ->
            var waitingTick = 0
            schedule {
                waitingTick++
                try {
                    if (condition.asBoolean) {
                        onResolve.executeVoid(waitingTick)
                        true
                    } else {
                        false
                    }
                } catch (e: Throwable) {
                    onReject.executeVoid(e)
                    true
                }
            }

            null
        }
    )

    /**
     * Example: `const result = await conditional(20, () => mc.player.isOnGround())`
     *
     * @return `Promise<number>`
     */
    @JvmOverloads
    @ScriptApiRequired
    fun conditional(
        ticks: Int,
        breakLoop: BooleanSupplier = BooleanSupplier { false }
    ): Value {
        if (ticks == 0) {
            return defaultPromise
        }

        var remains = ticks
        return until { --remains == 0 || breakLoop.asBoolean }
    }

}

