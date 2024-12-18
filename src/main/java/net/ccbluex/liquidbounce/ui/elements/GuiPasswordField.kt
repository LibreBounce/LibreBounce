/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.ui.elements

import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiTextField

class GuiPasswordField(
    componentId: Int,
    fontrendererObj: FontRenderer,
    x: Int,
    y: Int,
    width: Int,
    height: Int
) : GuiTextField(componentId, fontrendererObj, x, y, width, height) {

    /**
     * Draw text box
     */
    override fun drawTextBox() {
        val realText = text

        text = buildString(realText.length) {
            repeat(realText.length) {
                append('*')
            }
        }

        super.drawTextBox()

        text = realText
    }

}