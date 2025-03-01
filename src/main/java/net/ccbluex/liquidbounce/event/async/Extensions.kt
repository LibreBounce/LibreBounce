/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.event.async

import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.event.*
import kotlin.coroutines.resume


/**
 * Wait next event instance of given type which matches [predicate].
 *
 * Note: This might change thread context
 *
 * TODO: KNOWN BUG: following code will trigger exception:
 * ```
 * waitNext<PacketEvent>()
 * ...
 * sendPacket(...) // triggerEvents = true
 * ```
 *
 * @return the event instance
 */
suspend inline fun <reified E : Event> Listenable.waitNext(
    priority: Byte = 0,
    crossinline predicate: (E) -> Boolean = { true }
): E = suspendCancellableCoroutine { cont ->
    EventManager.registerTerminateEventHook(
        E::class.java,
        EventHook(this, always = false, priority) {
            if (predicate(it)) {
                cont.resume(it)
                true
            } else {
                false
            }
        }
    )
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
