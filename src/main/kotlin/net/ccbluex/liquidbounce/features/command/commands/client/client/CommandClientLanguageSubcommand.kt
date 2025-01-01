package net.ccbluex.liquidbounce.features.command.commands.client.client

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular

object CommandClientLanguageSubcommand {
    fun languageCommand() = CommandBuilder.begin("language")
        .hub()
        .subcommand(listSubcommand())
        .subcommand(setSubcommand())
        .subcommand(unsetSubcommand())
        .build()

    private fun unsetSubcommand() = CommandBuilder.begin("unset")
        .handler { command, args ->
            chat(regular("Unset override language..."))
            LanguageManager.overrideLanguage = ""
            ConfigSystem.storeConfigurable(LanguageManager)
        }.build()

    private fun setSubcommand() = CommandBuilder.begin("set")
        .parameter(
            ParameterBuilder.begin<String>("language")
                .autocompletedWith { begin, _ ->
                    LanguageManager.knownLanguages.filter { it.startsWith(begin, true) }
                }
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                .build()
        ).handler { command, args ->
            val language = LanguageManager.knownLanguages.find { it.equals(args[0] as String, true) }
            if (language == null) {
                chat(regular("Language not found."))
                return@handler
            }

            chat(regular("Setting language to ${language}..."))
            LanguageManager.overrideLanguage = language

            ConfigSystem.storeConfigurable(LanguageManager)
        }.build()

    private fun listSubcommand() = CommandBuilder.begin("list")
        .handler { command, args ->
            chat(regular("Available languages:"))
            for (language in LanguageManager.knownLanguages) {
                chat(regular("-> $language"))
            }
        }.build()
}
