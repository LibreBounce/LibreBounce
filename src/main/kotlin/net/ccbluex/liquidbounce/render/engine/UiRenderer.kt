package net.ccbluex.liquidbounce.render.engine

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.common.GlobalFrameBuffer
import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud.isBlurable
import net.ccbluex.liquidbounce.render.ui.ItemImageAtlas
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.render.DefaultFramebufferSet
import net.minecraft.client.render.RenderPhase
import net.minecraft.client.util.Pool
import net.minecraft.util.Identifier
import org.lwjgl.opengl.GL13
import kotlin.math.sin

object UiRenderer : MinecraftShortcuts {

    /**
     * UI Blur Post-Effect Processor
     *
     * @author superblaubeere27
     */
    private val BLUR = Identifier.of("liquidbounce", "ui_blur")
    private val pool = Pool(3)

    private var isDrawingHudFramebuffer = false

    private val overlayFramebuffer by lazy {
        val fb = SimpleFramebuffer(
            mc.window.framebufferWidth,
            mc.window.framebufferHeight,
            true
        )

        fb.setClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        fb
    }

    val OUTLINE_TARGET = RenderPhase.Target("overlay_target", {
        if (isDrawingHudFramebuffer) {
            overlayFramebuffer.beginWrite(true)
        }
    }, {})

    private val lastTimeScreenOpened = Chronometer()
    private var wasScreenOpen = false

    private fun easeFunction(x: Float): Float {
        return sin((x * Math.PI) / 2.0).toFloat()
    }

    private fun getBlurRadiusFactor(): Float {
        val isScreenOpen = mc.currentScreen != null && mc.currentScreen !is ChatScreen

        if (isScreenOpen && !wasScreenOpen) {
            lastTimeScreenOpened.reset()
        }

        wasScreenOpen = isScreenOpen

        return if (isScreenOpen) {
            easeFunction((lastTimeScreenOpened.elapsed.toFloat() / 500.0F + 0.1F).coerceIn(0.0F..1.0F))
        } else {
            1.0F
        }
    }

    private fun getBlurRadius(): Float {
        return (this.getBlurRadiusFactor() * 20.0F).coerceIn(5.0F..20.0F)
    }

    fun startUIOverlayDrawing(context: DrawContext, tickDelta: Float) {
        ItemImageAtlas.updateAtlas(context)

        if (isBlurable) {
            this.isDrawingHudFramebuffer = true

            this.overlayFramebuffer.clear()
            this.overlayFramebuffer.beginWrite(true)
            GlobalFrameBuffer.push(overlayFramebuffer)
        }

        callEvent(OverlayRenderEvent(context, tickDelta))
    }

    fun endUIOverlayDrawing() {
        if (!this.isDrawingHudFramebuffer) {
            return
        }

        this.isDrawingHudFramebuffer = false

        GlobalFrameBuffer.pop()
        this.overlayFramebuffer.endWrite()

        // Remember the previous projection matrix because the draw method changes it AND NEVER FUCKING CHANGES IT
        // BACK IN ORDER TO INTRODUCE HARD TO FUCKING FIND BUGS. Thanks Mojang :+1:
        val projectionMatrix = RenderSystem.getProjectionMatrix()
        val vertexSorting = RenderSystem.getProjectionType()

        blur()

        RenderSystem.enableBlend()
        RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA)

        this.overlayFramebuffer.drawInternal(mc.window.framebufferWidth, mc.window.framebufferHeight)

        RenderSystem.setProjectionMatrix(projectionMatrix, vertexSorting)
        RenderSystem.defaultBlendFunc()
    }

    fun blur() {
        RenderSystem.disableBlend()
//        RenderSystem.disableDepthTest()
//        RenderSystem.resetTextureMatrix()

        val overlayFramebuffer = overlayFramebuffer

        val postEffectProcessor = mc.shaderLoader.loadPostEffect(BLUR, DefaultFramebufferSet.MAIN_ONLY)!!

        val active = GlStateManager._getActiveTexture()
        GlStateManager._activeTexture(GL13.GL_TEXTURE9)
        GlStateManager._bindTexture(overlayFramebuffer.colorAttachment)
        postEffectProcessor.passes.first().program.getUniform("Overlay")!!.set(9)
        GlStateManager._activeTexture(active)

        postEffectProcessor.passes.first().program.getUniform("Radius")!!.set(getBlurRadius())

        postEffectProcessor.render(mc.framebuffer, pool)

        mc.framebuffer.beginWrite(true)
    }

    fun setupDimensions(width: Int, height: Int) {
        this.overlayFramebuffer.resize(width, height)
    }

}
