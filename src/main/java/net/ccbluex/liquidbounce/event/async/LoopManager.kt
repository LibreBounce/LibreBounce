/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.event.async

import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.event.*

import net.ccbluex.liquidbounce.utils.client.ClientUtils
import java.util.*

/**
 * This manager will start a job for each hook.
 *
 * Once the job is finished, the next [UpdateEvent] (any stateless event is OK for this) will start a new one.
 *
 * This is designed to run **asynchronous** tasks instead of tick loops.
 *
 * @author MukjepScarlet
 */
internal object LoopManager : Listenable, CoroutineScope by CoroutineScope(SupervisorJob()) {
    private val registry = IdentityHashMap<EventHook.Async<UpdateEvent>, Job?>()

    operator fun plusAssign(eventHook: EventHook.Async<UpdateEvent>) {
        registry[eventHook] = null
    }

    operator fun minusAssign(eventHook: EventHook.Async<UpdateEvent>) {
        registry.remove(eventHook)
    }

    init {
        handler<UpdateEvent>(priority = Byte.MAX_VALUE) {
            for ((eventHook, job) in registry) {
                if (eventHook.isActive) {
                    if (job == null || !job.isActive) {
                        registry[eventHook] = launch(eventHook.dispatcher) {
                            try {
                                eventHook.action(this, UpdateEvent)
                            } catch (e: CancellationException) {
                                // The job is canceled due to handler is no longer active
                                return@launch
                            } catch (e: Exception) {
                                ClientUtils.LOGGER.error("Exception during loop of ${eventHook.owner}", e)
                            }
                        }
                    }
                } else if (job != null) {
                    job.cancel()
                    registry[eventHook] = null
                }
            }
        }
    }
}
