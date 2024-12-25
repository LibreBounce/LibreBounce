package net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.google

import com.google.gson.GsonBuilder
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.ILanguages
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.Provider
import net.ccbluex.liquidbounce.utils.io.HttpClient
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Google : Provider {
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
    /**
     * All languages Google Translate supports
     * ! IMPORTANT: Use [id] as the language code, not the enum [name].
     */
    enum class Languages(override val id: String, override val choiceName: String) : ILanguages {
        AUTO("auto", "Detect language"),
        AF("af", "Afrikaans"),
        SQ("sq", "Albanian"),
        AM("am", "Amharic"),
        AR("ar", "Arabic"),
        HY("hy", "Armenian"),
        AS("as", "Assamese"),
        AY("ay", "Aymara"),
        AZ("az", "Azerbaijani"),
        BM("bm", "Bambara"),
        EU("eu", "Basque"),
        BE("be", "Belarusian"),
        BN("bn", "Bengali"),
        BHO("bho", "Bhojpuri"),
        BS("bs", "Bosnian"),
        BG("bg", "Bulgarian"),
        CA("ca", "Catalan"),
        CEB("ceb", "Cebuano"),
        NY("ny", "Chichewa"),
        ZH_CN("zh-CN", "Chinese (Simplified)"),
        ZH_TW("zh-TW", "Chinese (Traditional)"),
        CO("co", "Corsican"),
        HR("hr", "Croatian"),
        CS("cs", "Czech"),
        DA("da", "Danish"),
        DV("dv", "Dhivehi"),
        DOI("doi", "Dogri"),
        NL("nl", "Dutch"),
        EN("en", "English"),
        EO("eo", "Esperanto"),
        ET("et", "Estonian"),
        EE("ee", "Ewe"),
        TL("tl", "Filipino"),
        FI("fi", "Finnish"),
        FR("fr", "French"),
        FY("fy", "Frisian"),
        GL("gl", "Galician"),
        KA("ka", "Georgian"),
        DE("de", "German"),
        EL("el", "Greek"),
        GN("gn", "Guarani"),
        GU("gu", "Gujarati"),
        HT("ht", "Haitian Creole"),
        HA("ha", "Hausa"),
        HAW("haw", "Hawaiian"),
        IW("iw", "Hebrew"),
        HI("hi", "Hindi"),
        HMN("hmn", "Hmong"),
        HU("hu", "Hungarian"),
        IS("is", "Icelandic"),
        IG("ig", "Igbo"),
        ILO("ilo", "Ilocano"),
        ID("id", "Indonesian"),
        GA("ga", "Irish"),
        IT("it", "Italian"),
        JA("ja", "Japanese"),
        JW("jw", "Javanese"),
        KN("kn", "Kannada"),
        KK("kk", "Kazakh"),
        KM("km", "Khmer"),
        RW("rw", "Kinyarwanda"),
        GOM("gom", "Konkani"),
        KO("ko", "Korean"),
        KRI("kri", "Krio"),
        KU("ku", "Kurdish (Kurmanji)"),
        CKB("ckb", "Kurdish (Sorani)"),
        KY("ky", "Kyrgyz"),
        LO("lo", "Lao"),
        LA("la", "Latin"),
        LV("lv", "Latvian"),
        LN("ln", "Lingala"),
        LT("lt", "Lithuanian"),
        LG("lg", "Luganda"),
        LB("lb", "Luxembourgish"),
        MK("mk", "Macedonian"),
        MAI("mai", "Maithili"),
        MG("mg", "Malagasy"),
        MS("ms", "Malay"),
        ML("ml", "Malayalam"),
        MT("mt", "Maltese"),
        MI("mi", "Maori"),
        MR("mr", "Marathi"),
        MNI_MTEI("mni-Mtei", "Meiteilon (Manipuri)"),
        LUS("lus", "Mizo"),
        MN("mn", "Mongolian"),
        MY("my", "Myanmar (Burmese)"),
        NE("ne", "Nepali"),
        NO("no", "Norwegian"),
        OR("or", "Odia (Oriya)"),
        OM("om", "Oromo"),
        PS("ps", "Pashto"),
        FA("fa", "Persian"),
        PL("pl", "Polish"),
        PT("pt", "Portuguese"),
        PA("pa", "Punjabi"),
        QU("qu", "Quechua"),
        RO("ro", "Romanian"),
        RU("ru", "Russian"),
        SM("sm", "Samoan"),
        SA("sa", "Sanskrit"),
        GD("gd", "Scots Gaelic"),
        NSO("nso", "Sepedi"),
        SR("sr", "Serbian"),
        ST("st", "Sesotho"),
        SN("sn", "Shona"),
        SD("sd", "Sindhi"),
        SI("si", "Sinhala"),
        SK("sk", "Slovak"),
        SL("sl", "Slovenian"),
        SO("so", "Somali"),
        ES("es", "Spanish"),
        SU("su", "Sundanese"),
        SW("sw", "Swahili"),
        SV("sv", "Swedish"),
        TG("tg", "Tajik"),
        TA("ta", "Tamil"),
        TT("tt", "Tatar"),
        TE("te", "Telugu"),
        TH("th", "Thai"),
        TI("ti", "Tigrinya"),
        TS("ts", "Tsonga"),
        TR("tr", "Turkish"),
        TK("tk", "Turkmen"),
        AK("ak", "Twi"),
        UK("uk", "Ukrainian"),
        UR("ur", "Urdu"),
        UG("ug", "Uyghur"),
        UZ("uz", "Uzbek"),
        VI("vi", "Vietnamese"),
        CY("cy", "Welsh"),
        XH("xh", "Xhosa"),
        YI("yi", "Yiddish"),
        YO("yo", "Yoruba"),
        ZU("zu", "Zulu")
    }
}
