package net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.ModuleBetterChat

/**
 * BetterChat: Translate addition
 *
 * Uses Google Translate or DeepL to translate messages
 * Inspired by https://github.com/Vendicated/Vencord/tree/main/src/plugins/translate/
 */
object Translate : ToggleableConfigurable(ModuleBetterChat, "Translate", true) {
}
