package net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.deepl

interface ResponseData {
    val translations: List<Translations>
}
interface Translations {
    val detected_source_language: String
    val text: String
}
