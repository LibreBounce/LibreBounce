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
@file:Suppress("NOTHING_TO_INLINE")
package net.ccbluex.liquidbounce.utils.client.error

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.client.browseUrl
import net.ccbluex.liquidbounce.utils.client.error.errors.ClientError
import net.ccbluex.liquidbounce.utils.client.mc
import org.lwjgl.util.tinyfd.TinyFileDialogs
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.system.exitProcess

private typealias CurrentStringBuilder = StringBuilder

/**
 * The ErrorHandler class is responsible for handling and reporting errors encountered by the application.
 */
class ErrorHandler private constructor(
    private val error: Throwable,
    private val quickFix: QuickFix? = null,
    private val additionalMessage: String? = null,
    private val needToReport: Boolean = true
) {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun fatal(
            error: Throwable,
            quickFix: QuickFix? = null,
            needToReport: Boolean = true,
            additionalMessage: String? = null,
        ): Nothing {
            val finalQuickFix = if (error is ClientError && quickFix == null) {
                error.quickFix
            } else {
                quickFix
            }

            val finalNeedToReport = if (error is ClientError) {
                error.needToReport
            } else {
                needToReport
            }

            ErrorHandler(error, finalQuickFix, additionalMessage, finalNeedToReport).apply {
                if (buildAndShowMessage()) {
                    browseUrl("https://github.com/CCBlueX/LiquidBounce/issues/new?template=bug_report.yml")
                }

                exitProcess(1)
            }
        }
    }

    private inline val title get() = "${LiquidBounce.CLIENT_NAME} Nextgen"

    private val builder = CurrentStringBuilder()

    private inline fun header(): CurrentStringBuilder = builder.append(
        "$title has encountered an error!"
    )

    private inline fun quickFix(): CurrentStringBuilder = builder.apply {
        requireNotNull(quickFix)

        append(quickFix.description)
        appendLine(2)

        val messages = listOf(
            "What you need" to quickFix.whatYouNeed,
            "What to do" to quickFix.whatToDo
        ).filter { it.second != null }

        for ((index, message) in messages.withIndex()) {
            val (title, steps) = message

            requireNotNull(steps)

            append("${title}:")
            appendLine()
            appendQuickFixStep(steps)

            if (index < messages.lastIndex) {
                appendLine(2)
            }
        }
    }

    private inline fun reportMessage(): CurrentStringBuilder = builder.apply {
        append(
            """
                Try restarting the client.
                Please report this issue to the developers on GitHub if the error keeps occurring.

                Include the following information:
            """.trimIndent())
        appendLine(2)

        systemSpecs()
        appendLine()

        error()
        appendLine(2)

        append("Also include you game log, which can be found at:")
        appendLine()
        append((mc.runDirectory.toPath() / "logs" / "latest.log").absolutePathString())

        appendLine(2)
        append("Open new GitHub issue?")
    }

    private inline fun systemSpecs(): CurrentStringBuilder = builder.append(
        """
            OS: ${System.getProperty("os.name")} (${System.getProperty("os.arch")})
            Java: ${System.getProperty("java.version")}
            Client version: ${LiquidBounce.clientVersion} (${LiquidBounce.clientCommit})
        """.trimIndent()
    )

    private inline fun error(): CurrentStringBuilder = builder.apply {
        append("Error: ${error.message}")
        appendLine()
        append("Error type:  ${error.javaClass.name}")

        if (additionalMessage != null) {
            appendLine()
            append("Additional message: $additionalMessage")
        }
    }

    @Suppress("MagicNumber")
    fun buildAndShowMessage(): Boolean {
        builder.apply {
            header()
            appendLine(3)

            if (quickFix != null) {
                quickFix()
                appendLine(3)
            }

            if (needToReport) {
                reportMessage()
            } else {
                systemSpecs()
                appendLine()
                error()
            }
        }

        val message = builder.toString().replace("\"", "").replace("'", "")

        return if (needToReport) {
            TinyFileDialogs.tinyfd_messageBox(
                title,
                message,
                "yesno",
                "error",
                true
            )
        } else {
            TinyFileDialogs.tinyfd_messageBox(
                title,
                message,
                "ok",
                "error",
                true
            )

            false
        }
    }
}

private inline fun Appendable.appendQuickFixStep(quickFixStep: Steps): Appendable = apply {
    quickFixStep.steps
        .map {
            if (!it.endsWith(".")) {
                "$it."
            } else {
                it
            }
        }
        .withIndex()
        .joinToString("\n") { (index, line) ->
            val step = if (quickFixStep.showStep) {
                "${index + 1}."
            } else {
                "-"
            }

            "$step $line"
        }
        .let {
            if (it.isNotEmpty()) {
                append(it)
            }
        }
}

private inline fun Appendable.appendLine(times: Int = 1): Appendable = apply {
    repeat(times) {
        append('\n')
    }
}
