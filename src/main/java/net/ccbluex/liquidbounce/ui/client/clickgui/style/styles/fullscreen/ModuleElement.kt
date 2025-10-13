package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fullscreen

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.spacedValues
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.FullscreenStyle.accentColor
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.FullscreenStyle.elements
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.FullscreenStyle.mainColor
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.FullscreenStyle.referenceColor
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.BlackStyle.clickSound
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.extensions.addSpaces
import java.awt.Color

class ModuleElement(
    var module: Module,
    var startX: Float,
    var startY: Float = 0f,
    var previousElement: ModuleElement? = null)
{
    // add properties here name, description, enabled
    var name: String = ""
    var description: String = ""
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
        if (previousElement != null) this.startY = previousElement!!.startY + previousElement!!.height + margin

        showSettings = moduleSettingsState[name] ?: false

        updateValueElementsList()
    }

    fun drawElement() {
        updateModuleElement()

        drawRoundedRect(
            startX + margin,
            startY,
            startX + width - margin,
            startY + height,
            mainColor,
            3f
        )

        Fonts.fontSemibold40.drawString(module.name, startX + margin + 10, startY + 10,
            if (module.state) accentColor else Color.WHITE.rgb
        )

        Fonts.fontRegular30.drawString(module.description, startX + margin + 10, startY + Fonts.fontSemibold40.fontHeight + 15,
            referenceColor
        )

        if (showSettings) {
            updateValueElementsRendering()
            valueElements.forEach { it.drawElement() }
        }
    }

    fun handleClick(mouseX: Float, mouseY: Float, button: Int) {
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

            if (!showSettings) updateAllElementsLayout()
        }
    }

    private fun updateAllElementsLayout() {
        // TODO: Improve
        elements.forEach { element ->
            if (element.startY > startY - settingsHeight) return@forEach
            element.startY += settingsHeight - Fonts.fontSemibold40.fontHeight - 30f
        }
    }

    private fun updateModuleElement() {
        if (previousElement != null) {
            startY = previousElement!!.startY + previousElement!!.height + margin
            startX = previousElement!!.startX
        }
        toggleRangeY = startY..startY + 20f
        toggleRangeX = startX..startX + 290f
    }

    private fun updateValueElementsRendering() {
        if (valueElements.isEmpty()) return

        valueElements.first().startY = startY + Fonts.fontRegular40.fontHeight + 30f
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
                val valueName = if (spacedValues) value.name.addSpaces() else value.name

                // TODO: Add list, int & float ranges, string, and block values
                val newElement = when (value) {
                    is BoolValue -> BoolElement(value, valueName, startX + margin + 20, previousValue = previousElement)
                    is FloatValue -> FloatElement(value, valueName, startX + margin + 20, previousValue = previousElement)
                    is IntValue -> IntElement(value, valueName, startX + margin + 20, previousValue = previousElement)
                    //is FloatRangeValue -> FloatRangeElement(value, valueName, startX + margin + 20, previousValue = previousElement)
                    is IntRangeValue -> IntRangeElement(value, valueName, startX + margin + 20, previousValue = previousElement)
                    //is ListValue -> ListElement(value, valueName, startX + margin + 20, previousValue = previousElement)
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