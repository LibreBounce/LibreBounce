package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.panel

abstract class ValueElement {
    abstract var previousValue: ValueElement?
    abstract var startX: Float
    abstract var startY: Float
    abstract var width: Float
    abstract var height: Float
    abstract var margin: Float
    abstract fun drawElement(mouseX: Float, mouseY: Float, partialTicks: Float)
    abstract fun handleClick(mouseX: Float, mouseY: Float, button: Int)
}