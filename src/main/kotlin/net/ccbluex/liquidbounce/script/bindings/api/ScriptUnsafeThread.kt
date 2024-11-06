package net.ccbluex.liquidbounce.script.bindings.api

import kotlin.concurrent.thread

object ScriptUnsafeThread {

    @JvmName("run")
    fun run(callback: () -> Unit) = thread {
        callback()
    }
}
