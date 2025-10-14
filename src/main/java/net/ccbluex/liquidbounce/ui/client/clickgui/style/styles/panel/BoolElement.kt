/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.panel

import net.ccbluex.liquidbounce.config.BoolValue
import net.ccbluex.liquidbounce.ui.font.Fonts.fontRegular30
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.PanelStyle.highlightColor
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.PanelStyle.highlightColorAlpha
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.PanelStyle.referenceColor
import net.vitox.particle.util.RenderUtils.drawCircle
import java.awt.Color.WHITE

class BoolElement(
    var value: BoolValue,
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
        this.hitboxX = startX .. (startX + width + 14f)
        this.hitboxY = startY .. (startY + height-margin)
    }

    override fun drawElement() {
        updateElement()

        fontRegular30.drawString(
            valueName,
            startX,
            startY,
            WHITE.rgb
        )

        var circleY = startY + fontRegular30.fontHeight / 2f - 1.5f
        var circleX = startX + width + 10f

        if (value.isActive()) {
            drawCircle(
                circleX,
                circleY,
                1f,
                highlightColor
            )
        } else {
            drawCircle(circleX, circleY, 1f, referenceColor)
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