package net.ccbluex.liquidbounce.integration.theme.themes.liquidbounce.routes

import net.ccbluex.liquidbounce.integration.theme.themes.liquidbounce.LiquidBounceTheme
import net.ccbluex.liquidbounce.integration.theme.themes.liquidbounce.components.NativeComponent
import net.ccbluex.liquidbounce.integration.theme.type.native.NativeDrawableRoute
import net.minecraft.client.gui.DrawContext

class HudDrawableRoute : NativeDrawableRoute() {

    override fun render(context: DrawContext, delta: Float) {
        // Draw native components
        LiquidBounceTheme.components
            .filterIsInstance<NativeComponent>()
            .filter { it.enabled }
            .forEach { component ->
                val (width, height) = component.size()
                val box = component.alignment.getBounds(width.toFloat(), height.toFloat())

                context.matrices.push()
                context.matrices.translate(box.xMin.toDouble(), box.yMin.toDouble(), 0.0)
                component.render(context, delta)
                context.matrices.pop()
            }
    }

}
