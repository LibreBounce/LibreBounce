/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.event.async.LoopManager
import net.ccbluex.liquidbounce.event.async.TickScheduler
import net.ccbluex.liquidbounce.utils.client.ClientUtils
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.cancellation.CancellationException

/**
 * @see List.binarySearchBy
 */
private fun List<EventHook<*>>.findIndexByPriority(item: EventHook<*>): Int {
    var low = 0
    var high = size - 1

    while (low <= high) {
        val mid = (low + high).ushr(1)
        val midVal = get(mid)

        if (item.priority < midVal.priority)
            low = mid + 1
        else if (item.priority > midVal.priority)
            high = mid - 1
        else
            return mid
    }

    return low.inv()
}

/**
 * @author MukjepScarlet
 */
object EventManager : CoroutineScope by CoroutineScope(SupervisorJob()) {
    private val registry = ALL_EVENT_CLASSES.associateWithTo(IdentityHashMap(ALL_EVENT_CLASSES.size)) {
        CopyOnWriteArrayList<EventHook<in Event>>()
    }

    init {
        LoopManager
        TickScheduler
    }

    fun <T : Event> unregisterEventHook(eventClass: Class<out T>, eventHook: EventHook<in T>) {
        registry[eventClass]!!.remove(eventHook)
    }

    fun <T : Event> registerEventHook(eventClass: Class<out T>, eventHook: EventHook<T>): EventHook<T> {
        val container = registry[eventClass] ?: error("Unsupported Event type: ${eventClass.simpleName}")

        eventHook as EventHook<in Event>

        check(eventHook !in container) {
            "The EventHook of ${eventHook.owner} has already been registered"
        }

        val insertIndex = container.findIndexByPriority(eventHook).let {
            if (it < 0) it.inv() else it
        }
        container.add(insertIndex, eventHook)

        return eventHook
    }

    fun unregisterListener(listener: Listenable) {
        registry.values.forEach { it.removeIf { hook -> hook.owner == listener } }
    }

    private fun <T : Event> EventHook<T>.processEvent(event: T) {
        if (!this.isActive)
            return

        when (this) {
            is EventHook.Blocking -> {
                try {
                    action(event)
                } catch (e: Exception) {
                    ClientUtils.LOGGER.error("Exception during call event (blocking)", e)
                }
            }

            is EventHook.Terminate -> {
                try {
                    action(event)
                } catch (e: Exception) {
                    ClientUtils.LOGGER.error("Exception during call event (terminate, remaining=${this.remaining})", e)
                }
                if (this.shouldStop()) {
                    unregisterEventHook(event::class.java, this)
                }
            }

            is EventHook.Async -> {
                launch(dispatcher) {
                    try {
                        action(this, event)
                    } catch (e: Exception) {
                        ClientUtils.LOGGER.error("Exception during call event (async)", e)
                    }
                }
            }
        }
    }

    fun <T : Event> call(event: T): T {
        val hooks = registry[event.javaClass]!!

        hooks.forEach {
            it.processEvent(event)
        }

        return event
    }

    fun <T : Event> call(event: T, listener: Listenable): T {
        val hooks = registry[event.javaClass]!!

        hooks.forEach {
            if (it.owner == listener) {
                it.processEvent(event)
            }
        }

        return event
    }

}
