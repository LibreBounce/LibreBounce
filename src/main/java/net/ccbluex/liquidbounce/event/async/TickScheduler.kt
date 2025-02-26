/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.event.async

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.isActive
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import java.util.function.BooleanSupplier
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.RestrictsSuspension
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * This manager is for suspend tick functions.
 *
 * **ANY** scopes without [RestrictsSuspension] annotation can use wait actions.
 *
 * Note: These functions will be called on [Dispatchers.Main] (the Render thread).
 *
 * Most of the game events are called from the Render thread, except of [PacketEvent], it's called from the Netty client thread.
 * You should carefully use this to prevent unexpected thread issue.
 *
 * @author MukjepScarlet
 */
object TickScheduler : Listenable, MinecraftInstance {

    private val schedules = arrayListOf<BooleanSupplier>()

    init {
        handler<GameTickEvent>(priority = Byte.MAX_VALUE) {
            schedules.removeIf { it.asBoolean }
        }
    }

    /**
     * Add a task for scheduling.
     *
     * @param breakLoop Stop tick the body when it returns `true`
     */
    fun schedule(breakLoop: BooleanSupplier) {
        if (mc.isCallingFromMinecraftThread) {
            schedules += breakLoop
        } else {
            mc.addScheduledTask { schedules += breakLoop }
        }
    }
}

/**
 * Wait until given [condition] returns true.
 *
 * @param condition It will be called on [Dispatchers.Main] (the Render thread)
 * @return Total ticks during waiting
 */
suspend inline fun waitUntil(crossinline condition: () -> Boolean): Int =
    suspendCancellableCoroutine { cont ->
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
    suspendCancellableCoroutine { cont ->
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
 * Start a tick sequence for given [Listenable]
 * which will be cancelled if [Listenable.handleEvents] of the owner returns false
 */
fun Listenable.tickSequence(
    context: CoroutineContext = Dispatchers.Unconfined,
    body: suspend CoroutineScope.() -> Unit
) {
    val job = GlobalScope.launch(context, block = body)

    TickScheduler.schedule {
        when {
            !this@tickSequence.handleEvents() -> {
                job.cancel()
                true
            }
            else -> job.isCompleted
        }
    }
}
