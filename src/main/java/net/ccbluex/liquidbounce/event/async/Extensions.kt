/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.event.async

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import net.ccbluex.liquidbounce.event.*
import kotlin.coroutines.resume


/**
 * Take next event instance of given type and run [handler] with it.
 */
inline fun <reified E : Event> Listenable.takeNext(
    priority: Byte = 0,
    noinline handler: (E) -> Unit
) = EventManager.registerTerminateEventHook(
    E::class.java,
    EventHook(this, always = false, priority, handler)
)

/**
 * Wait next event instance of given type.
 *
 * Note: This might change thread context
 *
 * @return the event instance
 */
suspend inline fun <reified E : Event> Listenable.waitNext(
    priority: Byte = 0
): E = suspendCancellableCoroutine { cont ->
    takeNext(priority, cont::resume)
}

/**
 * Wait next event instance of given type which matches [predicate].
 *
 * @return the event instance
 */
suspend inline fun <reified E : Event> Listenable.waitNext(
    priority: Byte = 0,
    crossinline predicate: (E) -> Boolean
): E {
    var next: E
    while (true) {
        next = waitNext(priority)
        if (predicate(next)) {
            return next
        }
    }
}

/**
 * Start a tick sequence job for given [Listenable]
 * which will be cancelled if [Listenable.handleEvents] of the owner returns false
 */
fun Listenable.launchSequence(
    dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
    always: Boolean = false,
    body: suspend CoroutineScope.() -> Unit
) {
    val job = EventManager.launch(dispatcher, block = body)

    TickScheduler.schedule {
        if (!always && !this@launchSequence.handleEvents()) {
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
    always: Boolean = false,
    priority: Byte = 0,
    body: suspend CoroutineScope.() -> Unit
) {
    var job = EventManager.launch(dispatcher, block = body)

    handler<GameTickEvent>(always = true, priority) {
        if (!always && !this@loopSequence.handleEvents()) {
            job.cancel()
        } else if (!job.isActive) {
            job = EventManager.launch(dispatcher, block = body)
        }
    }
}
