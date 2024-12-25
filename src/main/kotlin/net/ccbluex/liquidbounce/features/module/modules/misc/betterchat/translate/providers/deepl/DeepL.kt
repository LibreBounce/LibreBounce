package net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.deepl

import com.google.gson.GsonBuilder
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.Provider
import java.net.HttpURLConnection
import java.net.URI

object DeepL : Provider("DeepL") {
    private val apiKey by text("API Key", "").doNotIncludeAlways()
    private val pro by boolean("Use Pro API", false)
    /**
     * Translates [text] to the specified language ([to]) from the language specified. ([from])
     * If [from] is `null`, the provider will try to detect the language.
     */
    override fun translate(text: String, to: String, from: String?): String {
        val apiUrl = when (pro) {
            true -> "https://api.deepl.com/v2/translate"
            false -> "https://api-free.deepl.com/v2/translate"
        }
        val connection = URI(apiUrl).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty(
            "Authorization",
            "DeepL-Auth-Key $apiKey"
        )
        connection.doOutput = true
        connection.doInput = true
        val outputStream = connection.outputStream
        outputStream.write(text.toByteArray())
        outputStream.close()
        connection.connect()
        val inputStream = connection.inputStream
        val reader = inputStream.bufferedReader()
        val gson = GsonBuilder().create()
        val responseObj = gson.fromJson(reader, ResponseData::class.java)
        return responseObj.translations.map { it.text }.filter { t -> t.isNotEmpty() }.joinToString(" ")
    }
}
