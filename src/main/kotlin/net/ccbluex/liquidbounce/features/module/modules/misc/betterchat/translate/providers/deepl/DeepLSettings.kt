package net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.deepl

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.Translate

object DeepLSettings : ToggleableConfigurable(Translate, "DeepL", false) {
    internal val apiKey by text("API Key", "").doNotIncludeAlways()
    internal val pro by boolean("Use Pro API", false)
}
