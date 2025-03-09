package net.ccbluex.liquidbounce.utils.client.error

class Steps(
    val showStep: Boolean,
    val steps: Array<String>
) {
    constructor(showStep: Boolean = false, vararg steps: String) : this(showStep, steps.toList().toTypedArray())
}

enum class QuickFix (
    val description: String,
    val whatYouNeed: Steps? = null,
    val stepsToFix: Steps? = null
) {
    JCEF_ISNT_COMPATIBLE_WITH_THAT_SYSTEM(
        "Your system isn't compatible",
        whatYouNeed = Steps(false,
            "A 64-bit computer",
            "Windows 10 or newer, macOS 10.15 or newer, or a Linux system"
        ),
        stepsToFix = Steps(false,
            "Please update your operating system to a never version"
        )
    ),
    DOWNLOAD_JCEF_FAILED(
        ""
    )
}
