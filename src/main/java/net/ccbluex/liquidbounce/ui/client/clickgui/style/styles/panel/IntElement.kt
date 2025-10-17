/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.panel

import net.ccbluex.liquidbounce.config.IntValue
import net.ccbluex.liquidbounce.ui.font.Fonts.fontRegular35
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.PanelStyle.highlightColor
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.PanelStyle.highlightColorAlpha
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.PanelStyle.referenceColor
import net.ccbluex.liquidbounce.utils.extensions.lerpWith
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.vitox.particle.util.RenderUtils.drawCircle
import java.awt.Color.WHITE

class IntElement(
    var value: IntValue,
    var valueName: String = "",
    override var startX: Float,
    override var startY: Float = 0f,
    override var previousValue: ValueElement? = null
) : ValueElement() {

    override var margin: Float = 5f

    override var height: Float = fontRegular35.fontHeight.toFloat() + margin
    override var width: Float = fontRegular35.getStringWidth(valueName).toFloat()

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

        fontRegular35.drawString(
            valueName,
            startX,
            startY,
            WHITE.rgb
        )

        val curValue = value.get().toFloat()
        val min = value.minimum
        val max = value.maximum
        val progress = (curValue - min) / (max - min)
        val offsetX = 100f * progress

        val circleX = startX + width + 10f + offsetX
        val circleY = startY + fontRegular35.fontHeight / 2f - 1.5f

        drawRect(
            startX + width + 10f,
            circleY - 0.75f,
            startX + width + 110f,
            circleY + 0.75f,
            referenceColor
        )

        drawCircle(circleX, circleY, 1.75f, highlightColor)

        fontRegular35.drawString(
            value.get().toString() + " " + (value.suffix ?: ""),
            startX + width + 120f,
            circleY - fontRegular35.fontHeight / 4f,
            WHITE.rgb
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