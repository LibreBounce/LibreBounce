package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fullscreen

import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.FullscreenStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.extensions.lerpWith
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.vitox.particle.util.RenderUtils.drawCircle
import net.ccbluex.liquidbounce.config.IntRangeValue
import java.awt.Color

class IntRangeElement(
    var value: IntRangeValue,
    var valueName = "",
    override var startX: Float,
    override var startY: Float = 0f,
    override var previousValue: ValueElement? = null
) : ValueElement() {

    override var margin: Float = 5f

    override var height: Float = Fonts.fontRegular35.fontHeight.toFloat() + margin
    override var width: Float = Fonts.fontRegular35.getStringWidth(valueName).toFloat()

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
        Fonts.fontRegular35.drawString(
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
        val circleY = startY + Fonts.fontRegular35.fontHeight / 2f - 1.5f

        drawCircle(firstCircleX, circleY, 3f, FullscreenStyle.highlightColorAlpha.rgb)
        drawCircle(firstCircleX, circleY, 1.5f, FullscreenStyle.highlightColor)

        drawRect(
            startX + width + 10f,
            circleY - 0.5f,
            startX + width + 110f,
            circleY + 0.5f,
            FullscreenStyle.referenceColor
        )

        drawCircle(lastCircleX, circleY, 3f, FullscreenStyle.highlightColorAlpha.rgb)
        drawCircle(lastCircleX, circleY, 1.5f, FullscreenStyle.highlightColor)

        Fonts.fontRegular30.drawString(
            "$first-$last" (value.suffix ?: ""),
            startX + width + 120f,
            circleY - Fonts.fontRegular30.fontHeight / 4f,
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
        if (button == 0 && hitboxX.contains(mouseX) && hitboxY.contains(mouseY)) {
            val min = startX + width + 10f
            val max = startX + width + 110f
            val progress = (mouseX - min) / (max - min)
            var newValue = value.lerpWith(progress)
            // Round to 2 decimal places
            value.set(newValue)
        }
    }
}