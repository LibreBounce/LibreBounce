package net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.deepl

import com.google.gson.GsonBuilder
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.ILanguages
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.Provider
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.deepl.DeepLSettings.apiKey
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.deepl.DeepLSettings.pro
import java.net.HttpURLConnection
import java.net.URI

object DeepL : Provider {
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
            "DeepLSettings-Auth-Key $apiKey"
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

    /**
     * All languages DeepL supports
     * ! IMPORTANT: Use [id] as the language code, not the enum [name].
     */
    enum class Languages(override val id: String, override val choiceName: String) : ILanguages {
        DETECT("" ,"Detect language"),
        AR("ar","Arabic"),
        BG("bg","Bulgarian"),
        ZH_HANS("zh-hans","Chinese (Simplified)"),
        ZH_HANT("zh-hant","Chinese (Traditional)"),
        CS("cs","Czech"),
        DA("da","Danish"),
        NL("nl","Dutch"),
        EN_US("en-us","English (American)"),
        EN_GB("en-gb","English (British)"),
        ET("et","Estonian"),
        FI("fi","Finnish"),
        FR("fr","French"),
        DE("de","German"),
        EL("el","Greek"),
        HU("hu","Hungarian"),
        ID("id","Indonesian"),
        IT("it","Italian"),
        JA("ja","Japanese"),
        KO("ko","Korean"),
        LV("lv","Latvian"),
        LT("lt","Lithuanian"),
        NB("nb","Norwegian"),
        PL("pl","Polish"),
        PT_BR("pt-br","Portuguese (Brazilian)"),
        PT_PT("pt-pt","Portuguese (European)"),
        RO("ro","Romanian"),
        RU("ru","Russian"),
        SK("sk","Slovak"),
        SL("sl","Slovenian"),
        ES("es","Spanish"),
        SV("sv","Swedish"),
        TR("tr","Turkish"),
        UK("uk","Ukrainian")
    }
}
