/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.panel

import net.ccbluex.liquidbounce.config.TextValue
import net.ccbluex.liquidbounce.ui.font.Fonts.fontRegular35
import java.awt.Color.WHITE

// TODO: Make this editable
class TextElement(
    var value: TextValue,
    var valueName: String = "",
    override var startX: Float,
    override var startY: Float = 0f,
    override var previousValue: ValueElement? = null
) : ValueElement() {

    private val string = "${valueName}: ${value.get()}"

    override var margin: Float = 5f
    override var height: Float = fontRegular35.fontHeight.toFloat() + margin
    override var width: Float = fontRegular35.getStringWidth(string).toFloat()

    private var hitboxX = 0f..0f
    private var hitboxY = 0f..0f

    init {
        if (previousValue != null) {
            startY = previousValue!!.startY + previousValue!!.height
        }

        this.hitboxX = startX .. (startX + width + 14f)
        this.hitboxY = startY .. (startY + height - margin)
    }

    override fun drawElement(mouseX: Float, mouseY: Float, partialTicks: Float) {
        updateElement()

        fontRegular35.drawString(
            string,
            startX,
            startY,
            WHITE.rgb
        )
    }

    private fun updateElement() {
        if (previousValue != null) {
            this.startY = previousValue!!.startY + previousValue!!.height
        }

        this.hitboxX = startX .. (startX + width + 14f)
        this.hitboxY = startY .. (startY + height - margin)
    }

    override fun handleClick(mouseX: Float, mouseY: Float, button: Int) {}
}