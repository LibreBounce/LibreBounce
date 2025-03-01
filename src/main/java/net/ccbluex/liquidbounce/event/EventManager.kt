/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.ccbluex.liquidbounce.event.async.TickScheduler
import net.ccbluex.liquidbounce.utils.client.ClientUtils
import java.util.*

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

internal inline fun <T : Any> createEventMap(valueSelector: (Class<out Event>) -> T): Map<Class<out Event>, T> =
    ALL_EVENT_CLASSES.associateWithTo(IdentityHashMap(ALL_EVENT_CLASSES.size), valueSelector)

/**
 * @author MukjepScarlet
 */
object EventManager : CoroutineScope by CoroutineScope(SupervisorJob()) {
    /**
     * All normal handlers (except of scripts) should be initialized at startup on the main thread
     */
    private val registry = createEventMap { ArrayList<EventHook<in Event>>() }

    /**
     * Terminate event hooks might be added from other threads
     * so we use the class of Event as the lock
     */
    private val terminateHooks = createEventMap {
        PriorityQueue<EventHook<in Event>>(11, Comparator.comparingInt { -it.priority })
    }

    /**
     * Prevent [ConcurrentModificationException]
     */
    private val incomingTerminateHooks = createEventMap { ArrayList<EventHook<in Event>>() }

    init {
        TickScheduler
    }

    fun <T : Event> unregisterEventHook(eventClass: Class<out T>, eventHook: EventHook<in T>): Boolean =
        synchronized(eventClass) {
            registry[eventClass]!!.remove(eventHook)
                    || terminateHooks[eventClass]!!.remove(eventHook)
                    || incomingTerminateHooks[eventClass]!!.remove(eventHook)
        }

    fun <T : Event> registerEventHook(eventClass: Class<out T>, eventHook: EventHook<T>): EventHook<T> {
        val container = registry[eventClass] ?: error("Unsupported Event type: ${eventClass.simpleName}")

        eventHook as EventHook<in Event>

        synchronized(eventClass) {
            check(eventHook !in container) {
                "The EventHook of ${eventHook.owner} has already been registered"
            }

            val insertIndex = container.findIndexByPriority(eventHook).let {
                if (it < 0) it.inv() else it
            }
            container.add(insertIndex, eventHook)
        }

        return eventHook
    }

    fun <T : Event> registerTerminateEventHook(eventClass: Class<out T>, eventHook: EventHook<T>): EventHook<T> {
        val container = incomingTerminateHooks[eventClass] ?: error("Unsupported Event type: ${eventClass.simpleName}")

        eventHook as EventHook<in Event>

        synchronized(eventClass) {
            check(eventHook !in container) {
                "The EventHook of ${eventHook.owner} has already been registered"
            }

            container.add(eventHook)
        }

        return eventHook
    }

    private fun <T : Event> EventHook<T>.processEvent(event: T) {
        if (!this.isActive)
            return

        try {
            action(event)
        } catch (e: Exception) {
            ClientUtils.LOGGER.error("Exception during processing event", e)
        }
    }

    fun <T : Event> call(event: T): T {
        val eventClass = event.javaClass

        synchronized(eventClass) {
            // Process terminate event hooks
            val terminate = terminateHooks[eventClass]!!
            terminate.removeIf {
                it.processEvent(event)
                true
            }
            val incoming = incomingTerminateHooks[eventClass]!!
            terminate.addAll(incoming)
            incoming.clear()
        }

        // Process normal event hooks
        val hooks = registry[eventClass]!!

        hooks.forEach {
            it.processEvent(event)
        }

        return event
    }

    fun <T : Event> call(event: T, listener: Listenable): T {
        val eventClass = event.javaClass

        synchronized(eventClass) {
            // Process terminate event hooks
            val terminate = terminateHooks[eventClass]!!
            terminate.removeIf {
                if (it.owner === listener) {
                    it.processEvent(event)
                    true
                } else {
                    false
                }
            }
            val incoming = incomingTerminateHooks[eventClass]!!
            incoming.removeIf {
                if (it.owner === listener) {
                    terminate += it
                    true
                } else {
                    false
                }
            }
        }

        val hooks = registry[event.javaClass]!!

        hooks.forEach {
            if (it.owner === listener) {
                it.processEvent(event)
            }
        }

        return event
    }

}
