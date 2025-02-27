/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.event.async

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.event.EventHook
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


/**
 * Wait until given [condition] returns true.
 *
 * @param condition It will be called on [Dispatchers.Main] (the Render thread)
 * @return Total ticks during waiting
 */
suspend inline fun waitUntil(crossinline condition: () -> Boolean): Int =
    suspendCoroutine { cont ->
        var waitingTick = 0
        TickScheduler.schedule {
            waitingTick++
            try {
                if (condition()) {
                    cont.resume(waitingTick)
                    true
                } else {
                    false
                }
            } catch (e: Throwable) {
                cont.resumeWithException(e)
                true
            }
        }
    }

/**
 * Wait for given [ticks].
 */
suspend fun waitTicks(ticks: Int) {
    require(ticks >= 0) { "Negative tick: $ticks" }

    if (ticks == 0) {
        return
    }

    var remainingTick = ticks
    waitUntil { --remainingTick == 0 }
}

/**
 * Wait next event of given type.
 *
 * Note: This might change thread context
 *
 * @return the event instance
 */
suspend inline fun <reified E : Event> waitNext(priority: Byte = 0): E =
    suspendCoroutine { cont ->
        EventManager.registerTerminateEventHook(
            E::class.java,
            EventHook.Blocking(TickScheduler, always = true, priority) {
                cont.resume(it)
            }
        )
    }

/**
 * Waits until the fixed amount of [ticks] ran out or the [callback] returns true.
 */
suspend inline fun waitConditional(
    ticks: Int,
    crossinline callback: (elapsedTicks: Int) -> Boolean
): Boolean {
    require(ticks >= 0) { "Negative tick: $ticks" }

    if (ticks == 0) {
        return true
    }

    var elapsedTicks = 0
    // `elapsedTicks` in 0 until `ticks`
    waitUntil { elapsedTicks >= ticks || callback(elapsedTicks++) }

    return elapsedTicks >= ticks
}

/**
 * Start a tick sequence job for given [Listenable]
 * which will be cancelled if [Listenable.handleEvents] of the owner returns false
 */
fun Listenable.launchSequence(
    dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
    body: suspend CoroutineScope.() -> Unit
) {
    val job = EventManager.launch(dispatcher, block = body)

    TickScheduler.schedule {
        if (!this@launchSequence.handleEvents()) {
            job.cancel()
            true
        } else {
            job.isCompleted
        }
    }
}

/**
 * Start a **looped** tick sequence job for given [Listenable]
 * which will be cancelled if [Listenable.handleEvents] of the owner returns false
 */
fun Listenable.loopSequence(
    dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
    body: suspend CoroutineScope.() -> Unit
) {
    var job = EventManager.launch(dispatcher, block = body)

    TickScheduler.schedule {
        if (!this@loopSequence.handleEvents()) {
            job.cancel()
            true
        } else {
            if (job.isCompleted) {
                job = EventManager.launch(dispatcher, block = body)
            }
            false
        }
    }
}
