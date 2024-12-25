package net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.ModuleBetterChat
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.Provider
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.deepl.DeepL
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.deepl.DeepLSettings
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.google.Google
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.google.GoogleSettings

/**
 * BetterChat: Translate addition
 *
 * Uses Google Translate or DeepL to translate messages
 * Inspired by https://github.com/Vendicated/Vencord/tree/main/src/plugins/translate/
 */
object Translate : ToggleableConfigurable(ModuleBetterChat, "Translate", true) {
    private val provider = enumChoice<TranslateProvider>("Provider", TranslateProvider.GOOGLE)
    // what a garbage
    private val toLanguage = enumChoice<Google.Languages>("To Language (Google)", Google.Languages.EN)
    private val fromLanguage by enumChoice<Google.Languages>("From Language (Google)", Google.Languages.EN)
    private val toLanguageDeepL = enumChoice<DeepL.Languages>("To Language (DeepL)", DeepL.Languages.EN_US)
    private val fromLanguageDeepL by enumChoice<DeepL.Languages>("From Language (DeepL)", DeepL.Languages.EN_US)
    enum class TranslateProvider(override val choiceName: String, val provider: Provider) : NamedChoice {
        GOOGLE("Google", Google),
        DEEPL("DeepL", DeepL)
    }
    init {
        tree(GoogleSettings)
        tree(DeepLSettings)
    }
    @Suppress("unused")
    val chatHandler = handler<ChatReceiveEvent> { event ->
        val message = event.message

//        provider.get().provider.translate(message, toLanguageDeepL, fromLanguageDeepL)
    }
}
