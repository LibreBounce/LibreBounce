package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fullscreen

import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.FullscreenStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.config.ListValue
import net.vitox.particle.util.RenderUtils.drawCircle
import java.awt.Color

class ListElement(
    var value: ListValue,
    var valueName: String = "",
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
            "${valueName}: ${value.get()}",
            startX,
            startY,
            Color.WHITE.rgb
        )
    }

    override fun handleClick(mouseX: Float, mouseY: Float, button: Int) {
        if (button == 0 && hitboxX.contains(mouseX) && hitboxY.contains(mouseY)) {
            cycle(button == 0)
        }
    }

    private fun cycle(next: Boolean) {
        val values = setting.values.toList()
        var index = values.indexOf(setting.get())

        index = if (next) {
            (index + 1) % values.size
        } else {
            (index - 1 + values.size) % values.size
        }

        setting.changeValue(values[index])
    }

    private fun updateElement() {
        if (previousValue != null) {
            this.startY = previousValue!!.startY + previousValue!!.height
        }

        this.hitboxX = startX .. (startX + width+14f)
        this.hitboxY = startY .. (startY + height-margin)
    }
}