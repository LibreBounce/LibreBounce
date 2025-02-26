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
import net.ccbluex.liquidbounce.event.async.waitTicks
import net.ccbluex.liquidbounce.utils.client.ClientUtils
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.PriorityBlockingQueue
import kotlin.Comparator
import kotlin.collections.ArrayList

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
        ArrayList<EventHook<in Event>>()
    }

    private val terminateHooks = ALL_EVENT_CLASSES.associateWithTo(IdentityHashMap(ALL_EVENT_CLASSES.size)) {
        PriorityBlockingQueue<EventHook<in Event>>(11, Comparator.comparingInt { -it.priority })
    }

    private class AsyncTask(val owner: EventHook<*>, val job: Job)

    private val jobs = CopyOnWriteArrayList<AsyncTask>()

    init {
        LoopManager
        TickScheduler

        LoopManager.loopHandler {
            jobs.removeIf { !it.owner.isActive || !it.job.isActive }
            waitTicks(1)
        }
    }

    fun Listenable.cancelAsyncJobs() {
        jobs.removeIf {
            if (it.owner.owner === this) {
                it.job.cancel()
                true
            } else {
                false
            }
        }
    }

    fun <T : Event> unregisterEventHook(eventClass: Class<out T>, eventHook: EventHook<in T>): Boolean =
        if (registry[eventClass]!!.remove(eventHook) || terminateHooks[eventClass]!!.remove(eventHook)) {
            jobs.removeIf {
                if (it.owner === eventHook) {
                    it.job.cancel()
                    true
                } else {
                    false
                }
            }
            true
        } else {
            false
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

    fun <T : Event> registerTerminateEventHook(eventClass: Class<out T>, eventHook: EventHook<T>): EventHook<T> {
        val container = terminateHooks[eventClass] ?: error("Unsupported Event type: ${eventClass.simpleName}")

        eventHook as EventHook<in Event>

        check(eventHook !in container) {
            "The EventHook of ${eventHook.owner} has already been registered"
        }

        container.add(eventHook)

        return eventHook
    }

    fun unregisterListener(listener: Listenable) {
        registry.values.forEach { it.removeIf { hook -> hook.owner === listener } }
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

            is EventHook.Async -> {
                val job = launch(dispatcher) {
                    try {
                        action(this, event)
                    } catch (e: Exception) {
                        ClientUtils.LOGGER.error("Exception during call event (async)", e)
                    }
                }
                jobs += AsyncTask(this, job)
            }
        }
    }

    fun <T : Event> call(event: T): T {
        val hooks = registry[event.javaClass]!!

        with(terminateHooks[event.javaClass]!!.iterator()) {
            while (hasNext()) {
                val hook = next()
                hook.processEvent(event)
                remove()
            }
        }

        hooks.forEach {
            it.processEvent(event)
        }

        return event
    }

    fun <T : Event> call(event: T, listener: Listenable): T {
        val hooks = registry[event.javaClass]!!

        with(terminateHooks[event.javaClass]!!.iterator()) {
            while (hasNext()) {
                val hook = next()
                if (hook.owner !== listener) {
                    continue
                }
                hook.processEvent(event)
                remove()
            }
        }

        hooks.forEach {
            if (it.owner === listener) {
                it.processEvent(event)
            }
        }

        return event
    }

}
