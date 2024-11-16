/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.render.shader

import com.mojang.blaze3d.platform.GlConst
import com.mojang.blaze3d.platform.GlStateManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gl.GlProgramManager
import net.minecraft.client.gl.GlUniform
import net.minecraft.client.gl.VertexBuffer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import org.lwjgl.opengl.GL30
import java.io.Closeable

/**
 * A GLSL shader renderer. Takes a vertex and fragment shader and renders it to the canvas.
 *
 * Inspired from the GLSL Panorama Shader Mod
 * https://github.com/magistermaks/mod-glsl
 */
open class Shader(vertex: String, fragment: String) : Closeable {

    protected var program = 0

    inner class UniformPointer(val name: String) {
        val pointer = GlUniform.getUniformLocation(program, name)
    }

    init {
        val vertProgram = compileShader(vertex, GlConst.GL_VERTEX_SHADER)
        val fragProgram = compileShader(fragment, GlConst.GL_FRAGMENT_SHADER)

        this.program = GlStateManager.glCreateProgram()

        GlStateManager.glAttachShader(program, vertProgram)
        GlStateManager.glAttachShader(program, fragProgram)
        GlStateManager.glLinkProgram(program)

        // Checks link status
        if (GlStateManager.glGetProgrami(program, GlConst.GL_LINK_STATUS) == GlConst.GL_FALSE) {
            val log = GlStateManager.glGetProgramInfoLog(program, 1024)
            error("Filed to link shader program! Caused by: $log")
        }

        // cleanup
        GlStateManager.glDeleteShader(vertProgram)
        GlStateManager.glDeleteShader(fragProgram)
    }

    private fun compileShader(source: String, type: Int): Int {
        val shader = GlStateManager.glCreateShader(type)
        GlStateManager.glShaderSource(shader, listOf(source))
        GlStateManager.glCompileShader(shader)

        // check compilation status
        if (GlStateManager.glGetShaderi(shader, GlConst.GL_COMPILE_STATUS) == GlConst.GL_FALSE) {
            val log = GlStateManager.glGetShaderInfoLog(shader, 1024)
            error("Filed to compile shader! Caused by: $log")
        }

        return shader
    }

    open fun use() {
        GlProgramManager.useProgram(this.program)
    }

    override fun close() {
        GlStateManager.glDeleteProgram(this.program)
    }

}

/**
 * A GLSL shader renderer. Takes a vertex and fragment shader and renders it to the canvas.
 *
 * Inspired from the GLSL Panorama Shader Mod
 * https://github.com/magistermaks/mod-glsl
 */
class CanvasShader(vertex: String, fragment: String) : Shader(vertex, fragment) {

    private var buffer = VertexBuffer(VertexBuffer.Usage.DYNAMIC)
    private var canvas = ScalableCanvas()

    private val timeLocation: Int
    private val mouseLocation: Int
    private val resolutionLocation: Int

    private var time = 0f

    init {
        // bake buffer data
        val builder = Tessellator.getInstance()
        val buffer = builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR)
        buffer.vertex(-1.0f, -1.0f, 1.0f).texture(0f, 0f)
            .color(1f, 1f, 1f, 1f)
        buffer.vertex(1.0f, -1.0f, 1.0f).texture(1f, 0f)
            .color(1f, 1f, 1f, 1f)
        buffer.vertex(1.0f, 1.0f, 1.0f).texture(1f, 1f)
            .color(1f, 1f, 1f, 1f)
        buffer.vertex(-1.0f, 1.0f, 1.0f).texture(0f, 1f)
            .color(1f, 1f, 1f, 1f)

        this.buffer.bind()
        this.buffer.upload(buffer.end())
        VertexBuffer.unbind()

        // get uniform pointers
        timeLocation = GlUniform.getUniformLocation(program, "time")
        mouseLocation = GlUniform.getUniformLocation(program, "mouse")
        resolutionLocation = GlUniform.getUniformLocation(program, "resolution")
    }

    fun draw(mouseX: Int, mouseY: Int, delta: Float) {
        use()

        canvas.resize(mc.window.framebufferWidth, mc.window.framebufferHeight)
        canvas.write()

        // update uniforms
        GL30.glUniform1f(timeLocation, time)
        time += (delta / 10f)
        GL30.glUniform2f(mouseLocation, mouseX.toFloat(), mouseY.toFloat())
        GL30.glUniform2f(resolutionLocation, canvas.width().toFloat(), canvas.height().toFloat())

        // draw
        buffer.bind()
        buffer.draw()
        canvas.blit(buffer)
    }

    override fun close() {
        super.close()
        buffer.close()
        canvas.close()
    }

}
