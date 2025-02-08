/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style

import net.ccbluex.liquidbounce.config.ColorValue
import net.ccbluex.liquidbounce.config.TextValue
import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.file.FileManager.valuesConfig
import net.ccbluex.liquidbounce.ui.client.clickgui.Panel
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.client.asResourceLocation
import net.ccbluex.liquidbounce.utils.client.playSound
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.timing.WaitTickUtils
import org.lwjgl.input.Mouse
import java.awt.Color
import java.math.BigDecimal
import kotlin.math.max

abstract class Style : MinecraftInstance {
    val rgbaLabels = listOf("R:", "G:", "B:", "A:")

    protected var sliderValueHeld: Value<*>? = null
        get() {
            if (!Mouse.isButtonDown(0)) field = null
            return field
        }
        set(value) {
            if (chosenText?.value != value) {
                chosenText = null
            }

            field = value
        }

    var chosenText: EditableText? = null

    abstract fun drawPanel(mouseX: Int, mouseY: Int, panel: Panel)
    abstract fun drawHoverText(mouseX: Int, mouseY: Int, text: String)
    abstract fun drawButtonElement(mouseX: Int, mouseY: Int, buttonElement: ButtonElement)
    abstract fun drawModuleElementAndClick(
        mouseX: Int,
        mouseY: Int,
        moduleElement: ModuleElement,
        mouseButton: Int?
    ): Boolean

    fun clickSound() {
        mc.playSound("gui.button.press".asResourceLocation())
    }

    fun showSettingsSound() {
        mc.playSound("random.bow".asResourceLocation())
    }

    protected fun round(v: Float): Float {
        var bigDecimal = BigDecimal(v.toString())
        bigDecimal = bigDecimal.setScale(2, 4)
        return bigDecimal.toFloat()
    }

    protected fun getHoverColor(color: Color, hover: Int, inactiveModule: Boolean = false): Int {
        val r = color.red - hover * 2
        val g = color.green - hover * 2
        val b = color.blue - hover * 2
        val alpha = if (inactiveModule) color.alpha.coerceAtMost(128) else color.alpha

        return Color(max(r, 0), max(g, 0), max(b, 0), alpha).rgb
    }

    fun <T> Value<T>.setAndSaveValueOnButtonRelease(new: T) {
        if (this is ColorValue) {
            changeValue(new)
        } else {
            set(new, false)
        }

        with(WaitTickUtils) {
            if (!hasScheduled(this)) {
                conditionalSchedule(this, 10) {
                    (sliderValueHeld == null).also { if (it) saveConfig(valuesConfig) }
                }
            }
        }
    }

    fun withDelayedSave(f: () -> Unit) {
        f()

        with(WaitTickUtils) {
            if (!hasScheduled(this)) {
                conditionalSchedule(this, 10) {
                    (sliderValueHeld == null).also { if (it) saveConfig(valuesConfig) }
                }
            }
        }
    }

    fun resetChosenText(value: Value<*>) {
        if (chosenText?.value == value) {
            chosenText = null
        }
    }

    fun moveRGBAIndexBy(delta: Int) {
        val chosenText = chosenText ?: return

        if (chosenText.value !is ColorValue) {
            return
        }

        this.chosenText = EditableText.forRGBA(chosenText.value, (chosenText.value.rgbaIndex + delta).mod(4))
    }
}

data class EditableText(
    val value: Value<*>,
    var string: String,
    var cursorIndex: Int = string.length,
    val validator: (String) -> Boolean = { true },
    val onUpdate: (String) -> Unit
) {
    var selectionStart: Int? = null
    var selectionEnd: Int? = null

    val cursorString get() = string.take(cursorIndex)

    fun updateText(newString: String) {
        if (validator(newString)) {
            onUpdate(newString)
        }
    }

    fun moveCursorBy(delta: Int) {
        cursorIndex = (cursorIndex + delta).coerceIn(0, string.length)
        clearSelection()
    }

    fun insertAtCursor(newText: String) {
        deleteSelectionIfActive()
        val newString = string.take(cursorIndex) + newText + string.drop(cursorIndex)
        if (validator(newString)) {
            string = newString
            cursorIndex += newText.length
        }
    }

    fun deleteAtCursor(length: Int) {
        if (selectionActive()) {
            deleteSelectionIfActive()
        } else if (cursorIndex > 0) {
            string = string.take(cursorIndex - length) + string.drop(cursorIndex)
            cursorIndex -= length
        }
    }

    fun selectAll() {
        selectionStart = 0
        selectionEnd = string.length
        cursorIndex = string.length
    }

    fun selectionActive() = selectionStart != null && selectionEnd != null

    private fun deleteSelectionIfActive() {
        if (selectionActive()) {
            val start = minOf(selectionStart!!, selectionEnd!!)
            val end = maxOf(selectionStart!!, selectionEnd!!)
            string = string.take(start) + string.drop(end)
            cursorIndex = start
            clearSelection()
        }
    }

    private fun clearSelection() {
        selectionStart = null
        selectionEnd = null
    }

    companion object {
        fun forTextValue(value: TextValue) = EditableText(
            value = value,
            string = value.get(),
            onUpdate = { value.set(it) }
        )

        fun forRGBA(value: ColorValue, index: Int): EditableText {
            val color = value.get()

            val component = when (index) {
                0 -> color.red
                1 -> color.green
                2 -> color.blue
                3 -> color.alpha
                else -> throw IllegalArgumentException("Invalid RGBA index")
            }

            value.rgbaIndex = index

            return EditableText(
                value = value,
                string = component.toString(),
                validator = {
                    ColorUtils.isValidColorInput(it)
                },
                onUpdate = { newText ->
                    val newValue = newText.toIntOrNull()?.coerceIn(0, 255) ?: component
                    val currentColor = value.get()
                    val newColor = when (index) {
                        0 -> Color(newValue, currentColor.green, currentColor.blue, currentColor.alpha)
                        1 -> Color(currentColor.red, newValue, currentColor.blue, currentColor.alpha)
                        2 -> Color(currentColor.red, currentColor.green, newValue, currentColor.alpha)
                        3 -> Color(currentColor.red, currentColor.green, currentColor.blue, newValue)
                        else -> currentColor
                    }
                    value.set(newColor)
                }
            )
        }
    }
}