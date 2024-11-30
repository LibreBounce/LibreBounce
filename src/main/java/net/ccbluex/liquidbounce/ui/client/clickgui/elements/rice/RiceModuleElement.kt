package net.ccbluex.liquidbounce.ui.client.clickgui.elements.rice

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.clickgui.RiceGui.accentColor
import net.ccbluex.liquidbounce.ui.client.clickgui.RiceGui.mainColor
import net.ccbluex.liquidbounce.ui.client.clickgui.RiceGui.referenceColor
import net.ccbluex.liquidbounce.ui.client.clickgui.RiceGui.visibleRange
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.BlackStyle.clickSound
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import java.awt.Color

class RiceModuleElement(
    var module: Module,
    var startX: Float,
    var startY: Float = 0f,
    var previousElement: RiceModuleElement? = null)
{
    // add properties here name, description, enabled
    var name: String = ""
    var description: String = ""
    var visible: Boolean = false
    var width: Float = 300f
    val margin: Float = 5f
    private var showSettings: Boolean = false
    var settingsHeight: Float = 0f
    var height: Float = if (showSettings) settingsHeight + margin * 2 else 40f
    private var toggleRangeY = startY..startY + 20f
    private var toggleRangeX = startX..startX + width
    private val valueElements = mutableListOf<ValueElement>()

    companion object {
        val moduleSettingsState = mutableMapOf<String, Boolean>()
    }

    init {
        this.name = module.name
        this.description = module.description
        this.visible = (startY in visibleRange) || ((startY+height) in visibleRange)
        if (previousElement != null) this.startY = previousElement!!.startY + previousElement!!.height + margin

        showSettings = moduleSettingsState[name] ?: false

        updateValueElementsList()
    }

    fun drawElement() {
        updateModuleElement()
        //if (!visible) return
        drawRoundedRect(
            startX + margin,
            startY,
            startX + width - margin,
            startY + height,
            mainColor,
            3f
        )
        Fonts.font40.drawString(module.name,startX + margin + 10, startY + 10,
            if (module.state) accentColor else Color.WHITE.rgb
        )
        Fonts.font30.drawString("(${module.category.displayName})",
            startX + margin + 15 + Fonts.font40.getStringWidth(module.name),
            startY + Fonts.font40.fontHeight - Fonts.font30.fontHeight + 8, referenceColor
        )
        Fonts.font30.drawString(module.description,startX + margin + 10, startY + Fonts.font40.fontHeight + 15,
            referenceColor
        )

        if (showSettings) {
            updateValueElementsRendering()
            valueElements.forEach { it.drawElement() }
        }
    }

    fun handleClick(mouseX: Float, mouseY: Float, button: Int) {
        //if (!visible) return
        if (button == 0) {
            if (toggleRangeX.contains(mouseX) && toggleRangeY.contains(mouseY)) {
                module.toggle()
                clickSound()
            }
            if (showSettings && toggleRangeX.contains(mouseX) && (startY..startY + height).contains(mouseY)) {
                valueElements.forEach { it.handleClick(mouseX, mouseY, button) }
                updateValueElementsList()
            }
        }
        if (button == 1 && toggleRangeX.contains(mouseX) && (startY..startY + height).contains(mouseY)) {
            showSettings = !showSettings
            height = if (showSettings) settingsHeight + margin * 2 else 40f

            moduleSettingsState[name] = showSettings
        }
    }

    private fun updateModuleElement() {
        if (previousElement != null) {
            this.startY = previousElement!!.startY + previousElement!!.height + margin
            this.startX = previousElement!!.startX
        }
        this.toggleRangeY = startY..startY + 20f
        this.toggleRangeX = startX..startX + 290f
        this.visible = (startY in visibleRange) || ((startY + height) in visibleRange)
    }

    private fun updateValueElementsRendering() {
        if (valueElements.isEmpty()) return

        valueElements.first().startY = startY + Fonts.font40.fontHeight + 30f
        valueElements.forEach { element ->
            element.startX = startX + margin + 10f
        }
    }

    private fun updateValueElementsList() {
        settingsHeight = 0f
        valueElements.clear()

        val moduleValues = module.values.filter { it.isSupported() }
        if (moduleValues.isNotEmpty()) {
            var previousElement: ValueElement? = null
            for (value in moduleValues) {
                val newElement = when (value) {
                    is BoolValue -> BoolValueElement(value, startX + margin + 20, previousValue = previousElement)
                    is FloatValue -> FloatValueElement(value, startX + margin + 20, previousValue = previousElement)
                    is IntegerValue -> IntValueElement(value, startX + margin + 20, previousValue = previousElement)
                    else -> null
                }
                newElement?.let { element ->
                    valueElements.add(element)
                    previousElement = element
                }
            }
        }
        valueElements.forEach { settingsHeight += it.height }
        settingsHeight += 30f
        height = if (showSettings) settingsHeight + margin * 2 else 40f
    }
}