package net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.google

import com.google.gson.GsonBuilder
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.Translate
import net.ccbluex.liquidbounce.utils.io.HttpClient
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object GoogleSettings : ToggleableConfigurable(Translate, "Google Translate", false) {

}
