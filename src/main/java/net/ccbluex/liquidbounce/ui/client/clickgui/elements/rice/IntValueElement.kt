package net.ccbluex.liquidbounce.ui.client.clickgui.elements.rice

import net.ccbluex.liquidbounce.ui.client.clickgui.RiceGui
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.extensions.lerpWith
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.IntegerValue
import java.awt.Color

class IntValueElement(
    var intValue: IntegerValue,
    override var startX: Float,
    override var startY: Float = 0f,
    override var previousValue: ValueElement? = null
) : ValueElement() {

    override var margin: Float = 5f

    override var height: Float = Fonts.font35.fontHeight.toFloat() + margin
    override var width: Float = Fonts.font35.getStringWidth(intValue.name).toFloat()

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
        Fonts.font35.drawString(
            intValue.name,
            startX,
            startY,
            Color.WHITE.rgb
        )

        val curValue = intValue.get().toFloat()
        val min = intValue.minimum
        val max = intValue.maximum
        val progress = (curValue - min) / (max - min)
        val offsetX = 100f * progress

        val circleX = startX + width + 10f + offsetX
        val circleY = startY + Fonts.font35.fontHeight / 2f - 1.5f

        RenderUtils.drawRect(
            startX + width + 10f,
            circleY - 0.5f,
            startX + width + 110f,
            circleY + 0.5f,
            RiceGui.referenceColor
        )
        net.vitox.particle.util.RenderUtils.drawCircle(circleX, circleY, 3f, RiceGui.highlightColorAlpha.rgb)
        net.vitox.particle.util.RenderUtils.drawCircle(circleX, circleY, 1.5f, RiceGui.highlightColor)

        Fonts.font30.drawString(
            intValue.get().toString(),
            startX + width + 120f,
            circleY - Fonts.font30.fontHeight / 4f,
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
            var newValue = intValue.lerpWith(progress)
            //round to 2 decimal places
            intValue.set(newValue)
        }
    }
}