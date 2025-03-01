/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.ccbluex.liquidbounce.event.async.TickScheduler
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.PriorityBlockingQueue

/**
 * @see List.binarySearchBy
 */
private fun List<EventHook<*, *>>.findIndexByPriority(item: EventHook<*, *>): Int {
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

internal inline fun <T : Any> createEventMap(valueSelector: (Class<out Event>) -> T): Map<Class<out Event>, T> =
    ALL_EVENT_CLASSES.associateWithTo(IdentityHashMap(ALL_EVENT_CLASSES.size), valueSelector)

/**
 * @author MukjepScarlet
 */
object EventManager : CoroutineScope by CoroutineScope(SupervisorJob()) {
    /**
     * All normal handlers (except of scripts) should be initialized at startup on the main thread
     */
    private val registry = createEventMap { ArrayList<EventHook<in Event, Unit>>() }

    /**
     * Terminate event hooks might be added from other threads
     */
    private val terminateHooks = createEventMap {
        PriorityQueue<EventHook<in Event, Boolean>>(11, Comparator.comparingInt { -it.priority })
    }

    /**
     * Prevent [ConcurrentModificationException]
     */
    private val incomingTerminateHooks = createEventMap { ArrayList<EventHook<in Event, Boolean>>() }

    init {
        TickScheduler
    }

    fun <T : Event> unregisterEventHook(eventClass: Class<out T>, eventHook: EventHook<in T, *>): Boolean =
        synchronized(eventClass) {
            registry[eventClass]!!.remove(eventHook)
                    || terminateHooks[eventClass]!!.remove(eventHook)
                    || incomingTerminateHooks[eventClass]!!.remove(eventHook)
        }

    // Only called from main thread
    fun <T : Event> registerEventHook(eventClass: Class<out T>, eventHook: EventHook<T, Unit>): EventHook<T, Unit> {
        val container = registry[eventClass] ?: error("Unsupported Event type: ${eventClass.simpleName}")

        eventHook as EventHook<in Event, Unit>

        check(eventHook !in container) {
            "The EventHook of ${eventHook.owner} has already been registered"
        }

        val insertIndex = container.findIndexByPriority(eventHook).let {
            if (it < 0) it.inv() else it
        }
        container.add(insertIndex, eventHook)

        return eventHook
    }

    fun <T : Event> registerTerminateEventHook(eventClass: Class<out T>, eventHook: EventHook<T, Boolean>): EventHook<T, Boolean> {
        val container = incomingTerminateHooks[eventClass] ?: error("Unsupported Event type: ${eventClass.simpleName}")

        eventHook as EventHook<in Event, Boolean>

        synchronized(container) {
            check(eventHook !in container) {
                "The EventHook of ${eventHook.owner} has already been registered"
            }

            container.add(eventHook)
        }

        return eventHook
    }

    fun <T : Event> call(event: T): T {
        val eventClass = event::class.java

        // Process terminate event hooks
        val terminate = terminateHooks[eventClass]!!
        val incoming = incomingTerminateHooks[eventClass]!!

        synchronized(incoming) {
            terminate.addAll(incoming)
            incoming.clear()
        }

        terminate.removeIf {
            it.processEvent(event) != false // null -> isNotActive, true -> shouldRemove
        }

        // Process normal event hooks
        val hooks = registry[eventClass]!!

        hooks.forEach {
            it.processEvent(event)
        }

        return event
    }

    fun <T : Event> call(event: T, listener: Listenable): T {
        val eventClass = event::class.java

        // Process terminate event hooks
        val terminate = terminateHooks[eventClass]!!
        val incoming = incomingTerminateHooks[eventClass]!!

        synchronized(incoming) {
            incoming.removeIf {
                if (it.owner === listener) {
                    terminate += it
                    true
                } else {
                    false
                }
            }
        }

        terminate.removeIf {
            if (it.owner === listener) {
                it.processEvent(event)
                true
            } else {
                false
            }
        }

        val hooks = registry[eventClass]!!

        hooks.forEach {
            if (it.owner === listener) {
                it.processEvent(event)
            }
        }

        return event
    }

}
