package net.ccbluex.liquidbounce.utils.client.error.errors

import net.ccbluex.liquidbounce.utils.client.error.QuickFix

object JcefIsntCompatible : ClientError(
    message = "JCEF Isn't compatible",
    quickFix = QuickFix.JCEF_ISNT_COMPATIBLE_WITH_THAT_SYSTEM,
    needToReport = false
) {
    fun readResolve(): Any = JcefIsntCompatible
}
