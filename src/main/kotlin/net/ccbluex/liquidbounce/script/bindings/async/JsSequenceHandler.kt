package net.ccbluex.liquidbounce.script.bindings.async

import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.SequenceManager
import net.ccbluex.liquidbounce.script.ScriptApiRequired
import net.ccbluex.liquidbounce.script.bindings.features.ScriptModule
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.util.function.BooleanSupplier
import java.util.function.Consumer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Transform Kotlin suspend functions to JS async functions (returns `Promise<T>`)
 *
 * @author MukjepScarlet
 */
internal class JsSequenceHandler(
    private val owner: ScriptModule,
    private val promiseConstructor: Value,
    private val asyncFunction: Value,
) {

    private var current: Sequence? = null

    fun startWith(eventInstance: Event?) {
        current = Sequence(owner) {
            /**
             * TODO
             * without this, the `asyncFunction.execute` will cause NPE
             * because the execution requires [current] to be initialized
             */
            sync()

            suspendCoroutine<Unit> { continuation ->
                // Promise<void>
                asyncFunction.execute(this@JsSequenceHandler, eventInstance).invokeMember(
                    "then",
                    Consumer(continuation::resume), // onResolve
                    Consumer(continuation::resumeWithException) // onRejected
                )
            }
        }
    }

    private inline fun <T> wrap(
        crossinline suspendableHandler: suspend Sequence.() -> T
    ): Value = promiseConstructor.newInstance(ProxyExecutable { (onResolve, onReject) ->
        SequenceManager.launch {
            try {
                val result = current!!.suspendableHandler()
                onResolve.execute(result)
            } catch (e: Throwable) {
                onReject.execute(e)
            }
        }
    })

    /**
     * Example: `await seq.ticks(10)`
     */
    @ScriptApiRequired
    fun ticks(n: Int) =
        wrap { waitTicks(n) }

    /**
     * Example: `await seq.seconds(1)`
     */
    @ScriptApiRequired
    fun seconds(n: Int) =
        wrap { waitSeconds(n) }

    /**
     * Example: `await seq.until(() => mc.player.isOnGround())`
     */
    @ScriptApiRequired
    fun until(condition: BooleanSupplier) =
        wrap { waitUntil(condition) }

    /**
     * Example: `const result = await seq.conditional(20, () => mc.player.isOnGround())`
     */
    @JvmOverloads
    @ScriptApiRequired
    fun conditional(ticks: Int, breakLoop: BooleanSupplier = BooleanSupplier { false }) =
        wrap { waitConditional(ticks, breakLoop) }

}
