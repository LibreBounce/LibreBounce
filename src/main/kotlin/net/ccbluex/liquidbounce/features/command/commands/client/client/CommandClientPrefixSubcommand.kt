package net.ccbluex.liquidbounce.features.command.commands.client.client

import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

object CommandClientPrefixSubcommand {
    fun prefixCommand() = CommandBuilder.begin("prefix")
        .parameter(
            ParameterBuilder
                .begin<String>("prefix")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .required()
                .build()
        )
        .handler { command, args ->
            val prefix = args[0] as String
            CommandManager.Options.prefix = prefix
            chat(regular(command.result("prefixChanged", variable(prefix))))
        }
        .build()
}
