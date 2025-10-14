/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.panel

import net.ccbluex.liquidbounce.config.RangeSlider
import net.ccbluex.liquidbounce.config.IntRangeValue
import net.ccbluex.liquidbounce.ui.font.Fonts.fontRegular30
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.PanelStyle.highlightColor
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.PanelStyle.highlightColorAlpha
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.PanelStyle.referenceColor
import net.ccbluex.liquidbounce.utils.extensions.lerpWith
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.vitox.particle.util.RenderUtils.drawCircle
import kotlin.math.abs
import java.awt.Color

class IntRangeElement(
    var value: IntRangeValue,
    var valueName: String = "",
    override var startX: Float,
    override var startY: Float = 0f,
    override var previousValue: ValueElement? = null
) : ValueElement() {

    override var margin: Float = 5f

    override var height: Float = fontRegular30.fontHeight.toFloat() + margin
    override var width: Float = fontRegular30.getStringWidth(valueName).toFloat()

    private var hitboxX = 0f..0f
    private var hitboxY = 0f..0f

    init {
        if (previousValue != null) {
            startY = previousValue!!.startY + previousValue!!.height
        }
        this.hitboxX = startX..(startX + width + 14f)
        this.hitboxY = startY..(startY + height - margin)
    }

    override fun drawElement() {
        updateElement()

        fontRegular30.drawString(
            valueName,
            startX,
            startY,
            Color.WHITE.rgb
        )

        val first = value.get().first
        val last = value.get().last
        val min = value.minimum
        val max = value.maximum
        val firstProgress = (first.toFloat() - min) / (max - min)
        val lastProgress = (last.toFloat() - min) / (max - min)
        val firstOffsetX = 100f * firstProgress
        val lastOffsetX = 100f * lastProgress

        val firstCircleX = startX + width + 10f + firstOffsetX
        val lastCircleX = startX + width + 10f + lastOffsetX
        val circleY = startY + fontRegular30.fontHeight / 2f - 1.5f

        drawCircle(firstCircleX, circleY, 3f, highlightColorAlpha.rgb)
        drawCircle(firstCircleX, circleY, 1.5f, highlightColor)

        drawRect(
            startX + width + 10f,
            circleY - 0.5f,
            startX + width + 110f,
            circleY + 0.5f,
            referenceColor
        )

        drawCircle(lastCircleX, circleY, 3f, highlightColorAlpha.rgb)
        drawCircle(lastCircleX, circleY, 1.5f, highlightColor)

        fontRegular30.drawString(
            "$first - $last" + " " + (value.suffix ?: ""),
            startX + width + 120f,
            circleY - fontRegular30.fontHeight / 4f,
            Color.WHITE.rgb
        )
    }

    private fun updateElement() {
        if (previousValue != null) {
            this.startY = previousValue!!.startY + previousValue!!.height
        }
        this.hitboxX = startX + width + 10f..(startX + width + 110f)
        this.hitboxY = startY..(startY + height - margin)
    }

    override fun handleClick(mouseX: Float, mouseY: Float, button: Int) {
        val slider1 = value.get().first
        val slider2 = value.get().last

        /*if (button == 0 && hitboxX.contains(mouseX) && hitboxY.contains(mouseY)) {
            val min = startX + width + 10f
            val max = startX + width + 110f
            val progress = (mouseX - min) / (max - min)
            var newValue = value.lerpWith(lastProgress)
            // Round to 2 decimal places
            value.setLast(newValue)
        }*/

        val otherStartX = startX + width + 10f
        //val startY = yPos + 14
        //val width = moduleElement.settingsWidth - 12

        val endX = startX + width + 110f

        val currSlider = value.lastChosenSlider

        //if (button == 0 && mouseX in startX..endX && mouseY in startY - 2..startY + 7 || sliderValueHeld == value) {
        if (button == 0 && mouseX in otherStartX..endX && mouseY in startY - 2..startY + 7) {
            val leftSliderPos =
                otherStartX + (slider1 - value.minimum).toFloat() / (value.maximum - value.minimum) * (endX - otherStartX)
            val rightSliderPos =
                otherStartX + (slider2 - value.minimum).toFloat() / (value.maximum - value.minimum) * (endX - otherStartX)

            val distToSlider1 = mouseX - leftSliderPos
            val distToSlider2 = mouseX - rightSliderPos

            val closerToLeft = abs(distToSlider1) < abs(distToSlider2)

            val isOnLeftSlider =
                (mouseX.toFloat() in otherStartX.toFloat()..leftSliderPos || closerToLeft) && rightSliderPos > otherStartX
            val isOnRightSlider =
                (mouseX.toFloat() in rightSliderPos..endX.toFloat() || !closerToLeft) && leftSliderPos < endX

            val percentage = (mouseX.toFloat() - otherStartX) / (endX - otherStartX)

            if (isOnLeftSlider && currSlider == null || currSlider == RangeSlider.LEFT) {
                value.setFirst(
                    value.lerpWith(percentage).coerceIn(value.minimum, slider2), false
                )
            }

            if (isOnRightSlider && currSlider == null || currSlider == RangeSlider.RIGHT) {
                value.setLast(
                    value.lerpWith(percentage).coerceIn(slider1, value.maximum), false
                )
            }

            // Keep changing this slider until mouse is unpressed.
            // sliderValueHeld = value

            // Stop rendering and interacting only when this event was triggered by a mouse click.
            if (button == 0) {
                value.lastChosenSlider = when {
                    isOnLeftSlider -> RangeSlider.LEFT
                    isOnRightSlider -> RangeSlider.RIGHT
                    else -> null
                }
                //return true
            }
        }
    }
}