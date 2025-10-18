/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.panel

import net.ccbluex.liquidbounce.config.ListValue
import net.ccbluex.liquidbounce.ui.font.Fonts.fontRegular35
import java.awt.Color.WHITE

class ListElement(
    var value: ListValue,
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

    override fun handleClick(mouseX: Float, mouseY: Float, button: Int) {
        if (button == 0 && hitboxX.contains(mouseX) && hitboxY.contains(mouseY)) {
            cycle(button == 0)
        }
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, state: Int) {}

    private fun cycle(next: Boolean) {
        val values = value.values.toList()
        var index = values.indexOf(value.get())

        index = if (next) {
            (index + 1) % values.size
        } else {
            (index - 1 + values.size) % values.size
        }

        value.changeValue(values[index])
    }

    private fun updateElement() {
        if (previousValue != null) {
            this.startY = previousValue!!.startY + previousValue!!.height
        }

        this.hitboxX = startX .. (startX + width + 14f)
        this.hitboxY = startY .. (startY + height - margin)
    }
}