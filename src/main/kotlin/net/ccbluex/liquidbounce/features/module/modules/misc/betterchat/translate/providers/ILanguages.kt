package net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers

import net.ccbluex.liquidbounce.config.types.NamedChoice

/**
 * All languages the provider with this interface supports
 * ! IMPORTANT: Use [id] as the language code, not the enum [name].
 */
interface ILanguages : NamedChoice {
    val id: String
}
