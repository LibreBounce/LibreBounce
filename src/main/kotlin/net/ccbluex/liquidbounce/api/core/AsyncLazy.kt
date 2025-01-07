package net.ccbluex.liquidbounce.api.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KProperty

class AsyncLazy<T>(
    private val initializer: suspend () -> T
) {
    private val deferred: CompletableDeferred<T> = CompletableDeferred()

    init {
        withScope {
            try {
                val result = initializer()
                deferred.complete(result)
            } catch (e: Throwable) {
                deferred.completeExceptionally(e)
            }
        }
    }

    suspend fun get(): T = deferred.await()

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        // Block on current thread if not called within a coroutine
        return runBlocking { get() }
    }

}
