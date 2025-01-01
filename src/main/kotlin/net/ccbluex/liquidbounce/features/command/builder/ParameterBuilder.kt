/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

package net.ccbluex.liquidbounce.features.command.builder

import net.ccbluex.liquidbounce.features.command.*
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.mc

class ParameterBuilder<T: Any> private constructor(val name: String) {

    private var verifier: ParameterVerificator<T>? = null
    private var required: Boolean? = null
    private var vararg: Boolean = false
    private var autocompletionHandler: AutoCompletionProvider? = null

    companion object {
        val STRING_VALIDATOR: ParameterVerificator<String> = ParameterVerificator { sourceText ->
            ParameterValidationResult.Ok(sourceText)
        }
        val MODULE_VALIDATOR: ParameterVerificator<ClientModule> = ParameterVerificator { sourceText ->
            val mod = ModuleManager.find { it.name.equals(sourceText, true) }

            if (mod == null) {
                ParameterValidationResult.error("Module '$sourceText' not found")
            } else {
                ParameterValidationResult.ok(mod)
            }
        }
        val INTEGER_VALIDATOR: ParameterVerificator<Int> = ParameterVerificator { sourceText ->
            try {
                ParameterValidationResult.ok(sourceText.toInt())
            } catch (e: NumberFormatException) {
                ParameterValidationResult.error("'$sourceText' is not a valid integer")
            }
        }
        val POSITIVE_INTEGER_VALIDATOR: ParameterVerificator<Int> = ParameterVerificator { sourceText ->
            try {
                val integer = sourceText.toInt()

                if (integer >= 0) {
                    ParameterValidationResult.ok(integer)
                } else {
                    ParameterValidationResult.error("The integer must be positive")
                }
            } catch (e: NumberFormatException) {
                ParameterValidationResult.error("'$sourceText' is not a valid integer")
            }
        }
        val BOOLEAN_VALIDATOR: ParameterVerificator<Boolean> = ParameterVerificator { sourceText ->
            when (sourceText.lowercase()) {
                "yes" -> ParameterValidationResult.ok(true)
                "no" -> ParameterValidationResult.ok(false)
                "true" -> ParameterValidationResult.ok(true)
                "false" -> ParameterValidationResult.ok(false)
                "on" -> ParameterValidationResult.ok(true)
                "off" -> ParameterValidationResult.ok(false)
                else -> ParameterValidationResult.error("'$sourceText' is not a valid boolean")
            }
        }

        fun <T: Any> begin(name: String): ParameterBuilder<T> = ParameterBuilder(name)
    }

    fun verifiedBy(verifier: ParameterVerificator<T>): ParameterBuilder<T> {
        this.verifier = verifier

        return this
    }

    fun optional(): ParameterBuilder<T> {
        this.required = false

        return this
    }

    /**
     * Marks this parameter as a vararg.
     *
     * The values are stored in an array
     *
     * Only allowed at the end.
     */
    fun vararg(): ParameterBuilder<T> {
        this.vararg = true

        return this
    }

    fun required(): ParameterBuilder<T> {
        this.required = true

        return this
    }

    fun autocompletedWith(autocompletionHandler: AutoCompletionProvider): ParameterBuilder<T> {
        this.autocompletionHandler = autocompletionHandler

        return this
    }

    fun useMinecraftAutoCompletion(): ParameterBuilder<T> {
        autocompletionHandler = AutoCompletionProvider { begin, _ ->
            mc.networkHandler?.playerList?.map { it.profile.name }?.filter { it.startsWith(begin, true) } ?: emptyList()
        }

        return this
    }

    fun build(): Parameter<T> {
        return Parameter(
            this.name,
            this.required
                ?: throw IllegalArgumentException("The parameter was neither marked as required nor as optional."),
            this.vararg,
            this.verifier,
            autocompletionHandler
        )
    }

}
