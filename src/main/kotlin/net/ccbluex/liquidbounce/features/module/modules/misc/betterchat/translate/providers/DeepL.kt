package net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers

object DeepL : Provider("DeepL") {
    /**
     * Translates [text] to the specified language ([to]) from the language specified. ([from])
     * If [from] is `null`, the provider will try to detect the language.
     */
    override fun translate(text: String, to: String, from: String?): String {
        TODO("Not yet implemented")
    }
}
