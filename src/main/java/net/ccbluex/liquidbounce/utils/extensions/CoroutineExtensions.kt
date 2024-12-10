package net.ccbluex.liquidbounce.utils.extensions

import kotlinx.coroutines.*

object SharedScopes {

    @JvmField
    val Default = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @JvmField
    val IO = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun stop() {
        Default.cancel()
        IO.cancel()
    }
}
