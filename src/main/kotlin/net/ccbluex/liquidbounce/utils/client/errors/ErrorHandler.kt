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
package net.ccbluex.liquidbounce.utils.client.errors

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.client.browseUrl
import org.lwjgl.util.tinyfd.TinyFileDialogs
import kotlin.system.exitProcess

/**
 * The ErrorHandler class is responsible for handling and reporting errors encountered by the application.
 */
class ErrorHandler private constructor(
    private val error: Throwable,
    private val quickFix: QuickFix? = null,
    private val additionalMessage: String? = null
) {
    companion object {
        @JvmStatic
        fun fatal(
            error: Throwable,
            quickFix: QuickFix? = null,
            additionalMessage: String? = null
        ) {
            ErrorHandler(error, quickFix, additionalMessage).apply {
                val message = """
                    ${buildMessage()}

                    Open GitHub issue?
                """.trimIndent()

                val openIssue = TinyFileDialogs.tinyfd_messageBox(
                    "${LiquidBounce.CLIENT_NAME}/${LiquidBounce.clientBranch}",
                    message,
                    "yesno",
                    "error",
                    true
                )

                if (openIssue) {
                    browseUrl("https://github.com/CCBlueX/LiquidBounce/issues/new?template=bug_report.yml")
                }

                exitProcess(1)
            }
        }
    }

    private val builder = StringBuffer()

    fun buildMessage(): StringBuilder {
        TODO("Build")
    }

}
