package net.ccbluex.liquidbounce.event

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

sealed class EventHook<T : Event>(
    val owner: Listenable,
    val always: Boolean,
    val priority: Byte,
) {
    val isActive: Boolean
        get() = this.owner.handleEvents() || this.always

    class Blocking<T : Event>(
        owner: Listenable,
        always: Boolean = false,
        priority: Byte = 0,
        val action: (T) -> Unit
    ) : EventHook<T>(owner, always, priority)

    class Terminate<T : Event>(
        owner: Listenable,
        always: Boolean = false,
        priority: Byte = 0,
        maxExecutionTime: Int = 1,
        val action: (T) -> Unit
    ) : EventHook<T>(owner, always, priority) {
        init {
            require(maxExecutionTime > 0)
        }

        var remaining = maxExecutionTime
            private set

        fun shouldStop(): Boolean = !isActive || remaining-- > 0
    }

    class Async<T : Event>(
        owner: Listenable,
        /**
         * Dispatcher Usage
         * - Unconfined: action will run blocking immediately, **unless a suspend function is called**
         * - Main: action will start to run at **next frame** on the main thread
         * - IO/Default: action will run at given dispatcher asynchronously
         *
         * If the event need to be canceled, don't set the dispatcher.
         */
        val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
        always: Boolean = false,
        priority: Byte = 0,
        val action: suspend CoroutineScope.(T) -> Unit
    ) : EventHook<T>(owner, always, priority)
}
