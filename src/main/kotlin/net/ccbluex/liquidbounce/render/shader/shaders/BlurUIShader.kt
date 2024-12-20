/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.render.shader.shaders

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.common.GlobalFrameBuffer
import net.ccbluex.liquidbounce.render.shader.FramebufferShader
import net.ccbluex.liquidbounce.render.shader.ProjMatUniform
import net.ccbluex.liquidbounce.render.shader.Shader
import net.ccbluex.liquidbounce.render.shader.UniformProvider
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.resourceToString
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.gl.VertexBuffer
import org.joml.Matrix4f
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20

object BlurUIShaderData {
    var radius = 5f
}

object BlurUIShader : FramebufferShader(Shader(
    resourceToString("/assets/liquidbounce/shaders/sobel.vert"),
    resourceToString("/assets/liquidbounce/shaders/ui_blur.frag"),
    arrayOf(
        ProjMatUniform,
        UniformProvider("Overlay") { pointer ->
            GlStateManager._activeTexture(GL13.GL_TEXTURE0)
            GL20.glUniform1i(pointer, 0)
        },
        UniformProvider("DiffuseSampler") { pointer ->
            GlStateManager._activeTexture(GL13.GL_TEXTURE5)
            GlStateManager._bindTexture(mc.framebuffer.colorAttachment)
            GL20.glUniform1i(pointer, 5)
        },
        UniformProvider("Radius") { pointer -> GL20.glUniform1f(pointer, /*BlurUIShaderData.radius*/20f) }
    )
)) {

    private val identity = Matrix4f()
    private val shaderProgram by lazy { mc.shaderLoader.getOrCreateProgram(ShaderProgramKeys.POSITION_TEX) }

    override fun enableBlend() {
        RenderSystem.disableBlend()
    }

    override fun endBlend() {
        RenderSystem.enableBlend()
    }

    fun endWrite() {
        GlobalFrameBuffer.pop()
        mc.framebuffer.beginWrite(true)
    }

    fun drawBuffer() {
        RenderSystem.enableBlend()
//        RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA)

        // Remember the previous projection matrix because the draw method changes it AND NEVER FUCKING CHANGES IT
        // BACK IN ORDER TO INTRODUCE HARD TO FUCKING FIND BUGS. Thanks Mojang :+1:
        val projectionMatrix = RenderSystem.getProjectionMatrix()
        val vertexSorting = RenderSystem.getProjectionType()

        buffer.bind()
        RenderSystem.setShaderTexture(0, framebuffers[0].colorAttachment)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        buffer.draw(identity, identity, shaderProgram)
        VertexBuffer.unbind()

        RenderSystem.defaultBlendFunc()
        RenderSystem.setProjectionMatrix(projectionMatrix, vertexSorting)
    }

}
