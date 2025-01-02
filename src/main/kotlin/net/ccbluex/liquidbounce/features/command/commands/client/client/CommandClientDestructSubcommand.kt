package net.ccbluex.liquidbounce.features.command.commands.client.client

import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder.Companion.BOOLEAN_VALIDATOR
import net.ccbluex.liquidbounce.features.misc.HideAppearance.destructClient
import net.ccbluex.liquidbounce.features.misc.HideAppearance.wipeClient
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.regular

object CommandClientDestructSubcommand {
    fun destructCommand() = CommandBuilder.begin("destruct")
        .parameter(
            ParameterBuilder.begin<Boolean>("confirm")
                .verifiedBy(BOOLEAN_VALIDATOR)
                .optional()
                .build()
        )
        .parameter(
            ParameterBuilder.begin<Boolean>("wipe")
                .verifiedBy(BOOLEAN_VALIDATOR)
                .optional()
                .build()
        )
        .handler { command, args ->
            val confirm = args.getOrNull(0) as Boolean? ?: false
            if (!confirm) {
                chat(
                    regular("Do you really want to destruct the client? " +
                    "If so, type the command again with 'yes' at the end.")
                )
                chat(markAsError("If you also want to wipe the client, add an additional 'yes' at the end."))
                chat(regular("For full destruct: .client destruct yes yes"))
                chat(regular("For temporary destruct: .client destruct yes"))
                return@handler
            }

            val wipe = args.getOrNull(1) as Boolean? ?: false

            chat(regular("LiquidBounce is being destructed from your client..."))
            if (!wipe) {
                chat(
                    regular("WARNING: You have not wiped the client (missing wipe parameter) - therefore " +
                    "some files may still be present!")
                )
            }

            destructClient()
            chat(
                regular("LiquidBounce has been destructed from your client. " +
                "You can clear your chat using F3+D. If wipe was enabled, the chat will be cleared automatically.")
            )

            if (wipe) {
                chat(regular("Wiping client..."))
                // Runs on a separate thread to prevent blocking the main thread and
                // repeating the process when required
                wipeClient()
            }
        }.build()
}
