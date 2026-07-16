package net.ccbluex.liquidbounce.utils.render

import net.minecraft.client.render.platform.GlStateManager
import net.minecraft.client.render.vertex.Tesselator
import net.minecraft.client.render.vertex.BufferBuilder

inline fun drawWithTesselatorWorldRenderer(drawAction: BufferBuilder.() -> Unit) {
    val instance = Tesselator.getInstance()
    try {
        instance.worldRenderer.drawAction()
    } finally {
        instance.draw()

        GlStateManager.resetColor()
    }
}
