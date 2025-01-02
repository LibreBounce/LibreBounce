package net.ccbluex.liquidbounce.features.command.commands.client.client

import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.util.Util

object CommandClientThemeSubcommand {
    fun themeCommand() = CommandBuilder.begin("theme")
        .hub()
        .subcommand(listSubcommand())
        .subcommand(setSubcommand())
        .subcommand(browseSubcommand())
        .build()

    private fun browseSubcommand() = CommandBuilder.begin("browse").handler { command, _ ->
        Util.getOperatingSystem().open(ThemeManager.themesFolder)
        chat(regular("Location: "), variable(ThemeManager.themesFolder.absolutePath))
    }.build()

    private fun setSubcommand() = CommandBuilder.begin("set")
        .parameter(
            ParameterBuilder.begin<String>("theme")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                .autocompletedWith { s, _ ->
                    ThemeManager.themes().filter { it.startsWith(s, true) }
                }
                .build()
        )
        .handler { command, args ->
            val name = args[0] as String

            if (name.equals("default", true)) {
                ThemeManager.activeTheme = ThemeManager.defaultTheme
                chat(regular("Switching theme to default..."))
                return@handler
            }

            runCatching {
                ThemeManager.chooseTheme(name)
            }.onFailure {
                chat(markAsError("Failed to switch theme: ${it.message}"))
            }.onSuccess {
                chat(regular("Switched theme to $name."))
            }
        }.build()

    private fun listSubcommand() = CommandBuilder.begin("list")
        .handler { command, args ->
            @Suppress("SpreadOperator")
            (chat(
                regular("Available themes: "),
                *ThemeManager.themes().flatMapIndexed { index, name ->
                    listOf(
                        regular(if (index == 0) "" else ", "),
                        variable(name)
                    )
                }.toTypedArray()
            ))
        }.build()
}
