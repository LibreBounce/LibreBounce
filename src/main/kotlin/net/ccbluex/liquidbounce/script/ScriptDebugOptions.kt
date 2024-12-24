package net.ccbluex.liquidbounce.script

data class ScriptDebugOptions(
    val enabled: Boolean = false,
    val protocol: DebugProtocol = DebugProtocol.INSPECT,
    val suspendOnStart: Boolean = false,
    val inspectInternals: Boolean = false
)

enum class DebugProtocol {
    DAP,
    INSPECT
}
