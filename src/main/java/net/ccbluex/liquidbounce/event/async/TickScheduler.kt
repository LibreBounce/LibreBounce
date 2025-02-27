/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.event.async

import kotlinx.coroutines.Dispatchers
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import java.util.function.BooleanSupplier
import kotlin.coroutines.*

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
