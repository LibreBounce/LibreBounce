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
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.render.engine.font.BoundingBox2f
import net.ccbluex.liquidbounce.utils.client.mc

data class Alignment(
    val horizontalAlignment: ScreenAxisX,
    val horizontalOffset: Int,
    val verticalAlignment: ScreenAxisY,
    val verticalOffset: Int,
) {

    constructor() : this(ScreenAxisX.LEFT, 0, ScreenAxisY.TOP, 0)

    fun getBounds(
        width: Float,
        height: Float,
        factor: Float = 1f
    ): BoundingBox2f {
        val screenWidth = mc.window.scaledWidth.toFloat()
        val screenHeight = mc.window.scaledHeight.toFloat()

        val horizontalOffset = (horizontalOffset * factor).toFloat()
        val x =
            when (horizontalAlignment) {
                ScreenAxisX.LEFT -> horizontalOffset
                ScreenAxisX.CENTER_TRANSLATED -> screenWidth / 2f - width / 2f + horizontalOffset
                ScreenAxisX.RIGHT -> screenWidth - width - horizontalOffset
                ScreenAxisX.CENTER -> screenWidth / 2f - width / 2f + horizontalOffset
            }

        val verticalOffset = (verticalOffset * factor).toFloat()
        val y =
            when (verticalAlignment) {
                ScreenAxisY.TOP -> verticalOffset
                ScreenAxisY.CENTER_TRANSLATED -> screenHeight / 2f - height / 2f + verticalOffset
                ScreenAxisY.BOTTOM -> screenHeight - height - verticalOffset
                ScreenAxisY.CENTER -> screenWidth / 2f - height / 2f + verticalOffset
            }

        return BoundingBox2f(x, y, x + width, y + height)
    }

    enum class ScreenAxisX(override val choiceName: String) : NamedChoice {
        LEFT("Left"),
        CENTER("Center"),
        CENTER_TRANSLATED("CenterTranslated"),
        RIGHT("Right"),
    }

    enum class ScreenAxisY(override val choiceName: String) : NamedChoice {
        TOP("Top"),
        CENTER("Center"),
        CENTER_TRANSLATED("CenterTranslated"),
        BOTTOM("Bottom"),
    }

    /**
     * Checks if the given point is inside the bounds of the alignment
     */
    fun contains(x: Float, y: Float, width: Float, height: Float): Boolean {
        val bounds = getBounds(width, height)
        return x >= bounds.xMin && x <= bounds.xMax && y >= bounds.yMin && y <= bounds.yMax
    }

    /**
     * Converts the alignement configurable to style (CSS)
     */
    @Deprecated("Style Transformation should be handled by the theme")
    fun toStyle() = """
        position: fixed;
        ${when (horizontalAlignment) {
        ScreenAxisX.LEFT -> "left: ${horizontalOffset}px"
        ScreenAxisX.RIGHT -> "right: ${horizontalOffset}px"
        ScreenAxisX.CENTER -> "left: calc(50% + ${horizontalOffset}px)"
        ScreenAxisX.CENTER_TRANSLATED -> "left: calc(50% + ${horizontalOffset}px)"
    }};
        ${when (verticalAlignment) {
        ScreenAxisY.TOP -> "top: ${verticalOffset}px"
        ScreenAxisY.BOTTOM -> "bottom: ${verticalOffset}px"
        ScreenAxisY.CENTER -> "top: calc(50% + ${verticalOffset}px)"
        ScreenAxisY.CENTER_TRANSLATED -> "top: calc(50% + ${verticalOffset}px)"
    }};
        transform: translate(
            ${if (horizontalAlignment == ScreenAxisX.CENTER_TRANSLATED) "-50%" else "0"},
            ${if (verticalAlignment == ScreenAxisY.CENTER_TRANSLATED) "-50%" else "0"}
        );
    """.trimIndent().replace("\n", "")

}
