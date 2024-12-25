package net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.google

import com.google.gson.GsonBuilder
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.Provider
import net.ccbluex.liquidbounce.utils.io.HttpClient
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Google : Provider("Google Translate") {
    private val BASE_TRANSLATE_URI = URI("https://translate.googleapis.com/translate_a/single")
    /**
     * Translates [text] to the specified language ([to]) from the language specified. ([from])
     * If [from] is `null`, the provider will try to detect the language.
     */
    override fun translate(text: String, to: String, from: String?): String {
        val params = mapOf(
            "client" to "gtx",
            "sl" to (from ?: "auto"),
            "tl" to to,
            "dt" to "t",
            "q" to text
        )
        val url = URI(
            BASE_TRANSLATE_URI.scheme,
            BASE_TRANSLATE_URI.host,
            BASE_TRANSLATE_URI.path,
            params.map {
                val key = URLEncoder.encode(it.key, StandardCharsets.UTF_8.toString())
                val value = URLEncoder.encode(it.value, StandardCharsets.UTF_8.toString())
                "$key=$value"
            }.joinToString("&"),
            null
        ).toURL()
        // the rest of this is basically just https://github.com/CCBlueX/ScriptAPI/blob/be5696c197f352560be85d3f4df19ba42b01371f/examples/translator.js#L47-L77
        val response = HttpClient.request(url.toString(), "GET")
        // deserialize the response using GSON
        val gson = GsonBuilder().create()
        val jsonResponse = gson.fromJson(response, ResponseData::class.java)

        // I don't know why, but I'm going to visualize this.
        return jsonResponse // { "src": "German", "sentences": [{ "trans": "A" }, {"trans": ""}, {"trans": "C"}] }
            .sentences // [{ "trans": "A" }, {"trans": ""}, {"trans": "C"}]
            .map { sentence -> sentence.trans } // ["A", "", "C"]
            .filter { translated -> translated.isNotEmpty() } // ["A", "C"]
            .joinToString(" ") // A C
    }
}
