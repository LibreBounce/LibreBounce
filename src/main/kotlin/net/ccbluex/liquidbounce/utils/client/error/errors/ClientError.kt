package net.ccbluex.liquidbounce.utils.client.error.errors

import net.ccbluex.liquidbounce.utils.client.error.QuickFix

open class ClientError(
    message: String = "",
    val quickFix: QuickFix? = null,
    val needToReport: Boolean = true
) : RuntimeException(message)
