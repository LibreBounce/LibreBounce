package net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.deepl

import com.google.gson.annotations.SerializedName

interface ResponseData {
    val translations: List<Translations>
}
interface Translations {
    @get:SerializedName("detected_source_language")
    val detectedSourceLanguage: String
    val text: String
}
