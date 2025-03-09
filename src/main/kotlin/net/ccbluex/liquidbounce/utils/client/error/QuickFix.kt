package net.ccbluex.liquidbounce.utils.client.error

class Steps(
    val showStepIndex: Boolean,
    val steps: Array<String>
)

enum class QuickFix (
    val description: String,
    val whatYouNeed: Steps? = null,
    val whatToDo: Steps? = null
) {
    JCEF_ISNT_COMPATIBLE_WITH_THAT_SYSTEM(
        "Your system isn't compatible with JCEF",
        whatYouNeed = Steps(false, arrayOf(
            "A 64-bit computer",
            "Windows 10 or newer, macOS 10.15 or newer, or a Linux system"
        )),
        whatToDo = Steps(false, arrayOf(
            "Please update your operating system to a never version"
        ))
    ),
    DOWNLOAD_JCEF_FAILED(
        "A fatal error occurred while loading libraries required for JCEF to work",
        whatYouNeed = Steps(true, arrayOf(
            "Stable internet connection",
            "Free space on the disk"
        )),
        whatToDo = Steps(true, arrayOf(
            "Check your internet connection",
            "Use a VPN such as Cloudflare Warp or another one",
            "Check if there is free space on the disk",
            "Make sure that the client folder is not blocked by the file system"
        ))
    );

    val messages = listOf(
        "What you need" to whatYouNeed,
        "What to do" to whatToDo
    ).filter { it.second != null }
}
