/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.event

abstract class Event

abstract class CancellableEvent : Event() {

    /**
     * Let you know if the event is cancelled
     *
     * @return state of cancel
     */
    var isCancelled = false
        private set

    /**
     * Allows you to cancel an event
     *
     * Note: It doesn't make sense to cancel a event within async handlers
     */
    fun cancelEvent() {
        isCancelled = true
    }

}

enum class EventState(val stateName: String) {
    PRE("PRE"), POST("POST"), // MotionEvent
    SEND("SEND"), RECEIVE("RECEIVE") // PacketEvent
}