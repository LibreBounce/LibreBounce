package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fullscreen

import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.FullscreenStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.config.BoolValue
import net.vitox.particle.util.RenderUtils.drawCircle
import java.awt.Color

class BoolElement(
    var value: BoolValue,
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
        this.hitboxX = startX .. (startX + width + 14f)
        this.hitboxY = startY .. (startY + height-margin)
    }

    override fun drawElement() {
        updateElement()

        Fonts.fontRegular35.drawString(
            valueName,
            startX,
            startY,
            Color.WHITE.rgb
        )

        var circleY = startY + Fonts.fontRegular35.fontHeight / 2f - 1.5f
        var circleX = startX + width + 10f

        if (value.isActive()) {
            drawCircle(circleX, circleY, 4f, FullscreenStyle.highlightColorAlpha.rgb)
            drawCircle(
                circleX,
                circleY,
                2f,
                FullscreenStyle.highlightColor
            )
        } else {
            drawCircle(circleX, circleY, 3f, FullscreenStyle.referenceColor)
        }
    }

    override fun handleClick(mouseX: Float, mouseY: Float, button: Int) {
        if (button == 0 && hitboxX.contains(mouseX) && hitboxY.contains(mouseY)) {
            value.toggle()
        }
    }

    private fun updateElement() {
        if (previousValue != null) {
            this.startY = previousValue!!.startY + previousValue!!.height
        }
        this.hitboxX = startX .. (startX + width+14f)
        this.hitboxY = startY .. (startY + height-margin)
    }
}