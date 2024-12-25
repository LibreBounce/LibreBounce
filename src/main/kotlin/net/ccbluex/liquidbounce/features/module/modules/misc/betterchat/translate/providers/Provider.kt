package net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers

/**
 * Interface for describing a translation provider named [name].
 * @see [DeepL]
 * @see [net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.google.Google]
 */
abstract class Provider(name: String) {
    /**
     * Translates [text] to the specified language ([to]) from the language specified. ([from])
     * If [from] is `null`, the provider will try to detect the language.
     */
    abstract fun translate(text: String, to: String, from: String?): String
}
