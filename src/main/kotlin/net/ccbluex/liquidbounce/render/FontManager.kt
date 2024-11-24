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
package net.ccbluex.liquidbounce.render

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.render.engine.font.FontGlyphPageManager
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.utils.client.logger
import org.apache.commons.exec.OS
import java.awt.Font
import java.io.File

object FontManager {

    /**
     * As fallback, we can use a common font that is available on all systems.
     */
    private val COMMON_FONT = runCatching {
        when {
            OS.isFamilyWindows() -> systemFont("Segoe UI")
            OS.isFamilyMac() -> systemFont("Helvetica")
            OS.isFamilyUnix() -> systemFont("DejaVu Sans")
            else -> systemFont("Arial")
        }
    }.onFailure { throwable ->
        logger.error("Failed to load common font.", throwable)
    }.getOrNull() ?: systemFont("Arial")

    /**
     * All font faces that are known to the font manager.
     */
    internal val fontFaces = mutableSetOf(
        COMMON_FONT
    )

    /**
     * The active font renderer that all text rendering will be based on.
     *
     * TODO: Because PR #3884 is not merged yet, we have to define the active font renderer manually.
     *    This will be removed once the PR is merged, because after the PR is merged, the font renderer
     *    can be selected through the module settings instead.
     */
    val FONT_RENDERER
        get() = (fontFace("Inter Regular") ?: COMMON_FONT).getRenderer()

    /**
     * Since our font renderer does not support dynamic font size changes,
     * we will use 43 as the default font size.
     */
    const val DEFAULT_FONT_SIZE: Float = 43f

    /**
     * Returns the font by the given name.
     */
    internal fun fontFace(name: String) = fontFaces.associateBy { fontFace -> fontFace.name }[name]

    internal suspend fun workOnQueue() = coroutineScope {
        fontFaces.map { fontFace ->
            // Launch coroutine scope for each font face to load them concurrently in the same-thread context.
            // This is necessary because the font loading requires GL context.
            launch {
                fontFace.make()
            }
        }.forEach { job -> job.join() }
    }

    internal fun queueFolder(path: File) {
        try {
            path.listFiles { file -> file.extension == "ttf" }
                ?.forEach(::queueFile)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load font from folder $path", e)
        }
    }

    internal fun queueFile(file: File) {
        try {
            if (!file.exists()) {
                logger.warn("Font file ${file.absolutePath} does not exist.")
                return
            }

            if (file.extension != "ttf") {
                logger.warn("Font file ${file.absolutePath} is not a TrueType font.")
                return
            }

            if (fontFaces.any { it.file == file }) {
                logger.warn("Font file ${file.absolutePath} is already loaded.")
                return
            }

            val font = Font
                .createFont(Font.TRUETYPE_FONT, file)
                .deriveFont(DEFAULT_FONT_SIZE)

            // Name will consist of the font name and family. This makes it possible
            // to select the different styles of the font.
            val fontFace = FontFace(font.name, file)
            // In this case, we have only one style available, which is the plain style.
            fontFace.fillStyle(font, 0)
            fontFaces += fontFace
        } catch (e: Exception) {
            logger.warn("Failed to load font from file ${file.absolutePath}", e)
        }
    }

    private fun systemFont(name: String): FontFace {
        val fontFace = FontFace(name)

        arrayOf(
            Font.BOLD,
            Font.BOLD,
            Font.ITALIC,
            Font.BOLD or Font.ITALIC
        ).map { style ->
            Font(name, style, DEFAULT_FONT_SIZE.toInt())
                .deriveFont(DEFAULT_FONT_SIZE)
        }.forEachIndexed { index, font ->
            fontFace.fillStyle(font, index)
        }

        return fontFace
    }

    internal data class FontFace(
        val name: String,
        /**
         * The file of the font. If the font is a system font, this will be null.
         */
        val file: File? = null,
        @Suppress("ArrayInDataClass")
        /**
         * Style of the font. If an element is null, fall back to `[0]`
         *
         * [Font.PLAIN] -> 0 (Must not be null)
         *
         * [Font.BOLD] -> 1 (Can be null)
         *
         * [Font.ITALIC] -> 2 (Can be null)
         *
         * [Font.BOLD] | [Font.ITALIC] -> 3 (Can be null)
         */
        val fontStyles: Array<Font?> = arrayOfNulls(4)
    ) {

        private var renderer: FontRenderer? = null
        internal val isLoaded get() = renderer != null

        fun getRenderer() = renderer ?: error("Font was not loaded yet!")

        /**
         * Fills the font style at the given index.
         */
        fun fillStyle(style: Font, index: Int) {
            fontStyles[index] = style
        }

        /**
         * Creates a [FontRenderer] instance from the given font styles.
         */
        fun make() {
            if (isLoaded) {
                return
            }

            renderer = FontRenderer(
                fontStyles.map { it?.let { FontGlyphPageManager(it) } }.toTypedArray(),
                DEFAULT_FONT_SIZE
            )
        }


    }

}
