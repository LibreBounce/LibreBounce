/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular

/**
 * Value Command
 *
 * Allows you to set the value of a specific module.
 */
object CommandValue : CommandFactory {

    @Suppress("SwallowedException", "LongMethod")
    override fun createCommand(): Command {
        return CommandBuilder
            .begin("value")
            .parameter(
                ParameterBuilder
                    .begin<ClientModule>("moduleName")
                    .verifiedBy(ParameterBuilder.MODULE_VALIDATOR)
                    .autocompletedWith { begin, _ -> ModuleManager.autoComplete(begin) }
                    .required()
                    .build()
            )
            .parameter(
                ParameterBuilder
                    .begin<String>("valueName")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .autocompletedWith { begin, args ->
                        val module = ModuleManager.find { it.name.equals(args[1], true) }
                        if (module == null) return@autocompletedWith emptyList()

                        module.getContainedValuesRecursively()
                            .filter { it.name.startsWith(begin, true) }
                            .map { it.name }
                    }
                    .required()
                    .build()
            )
            .parameter(
                ParameterBuilder
                    .begin<String>("value")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .autocompletedWith { begin, args ->
                        val module = args.getOrNull(1)
                            ?.let { moduleName -> ModuleManager.find { it.name.equals(moduleName, true) } }
                            ?: return@autocompletedWith emptyList()

                        val value = args.getOrNull(2)
                            ?.let { valueName -> module.getContainedValuesRecursively()
                                .firstOrNull { it.name.equals(valueName, true) }
                            } ?: return@autocompletedWith emptyList()

                        @Suppress("USELESS_IS_CHECK")
                        return@autocompletedWith when (value) {
                            is ChoiceConfigurable<*> -> value.choices.mapNotNull {
                                it.choiceName.takeIf { n -> n.startsWith(begin, true) }
                            }
                            is Value -> {
                                if (value.getValue() is Boolean) {
                                    arrayOf("true", "false").filter { it.startsWith(begin, true) }
                                } else {
                                    emptyList()
                                }
                            }
                        }
                    }
                    .required()
                    .build()
            )
            .handler { command, args ->
                val module = args[0] as ClientModule
                val valueName = args[1] as String
                val valueString = args[2] as String

                val value = module.getContainedValuesRecursively()
                    .firstOrNull { it.name.equals(valueName, true) }
                    ?: throw CommandException(command.result("valueNotFound", valueName))

                try {
                    value.setByString(valueString)
                    ModuleClickGui.reloadView()
                } catch (e: Exception) {
                    throw CommandException(command.result("valueError", valueName, e.message ?: ""))
                }

                chat(
                    regular(command.result("success")),
                    metadata = MessageMetadata(id = "CValue#success${module.name}")
                )
            }
            .build()
    }
}
