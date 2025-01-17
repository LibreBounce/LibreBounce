package net.ccbluex.liquidbounce.utils.render.shader.shaders

import net.ccbluex.liquidbounce.utils.render.shader.FramebufferShader
import org.lwjgl.opengl.GL20.glUniform1f
import org.lwjgl.opengl.GL20.glUniform2f
import org.lwjgl.opengl.GL20.glUniform1i

object FrostShader : FramebufferShader("frost.frag") {
    
    override fun setupUniforms() {
        setupUniform("texture")
        setupUniform("texelSize")
        setupUniform("radius")
        setupUniform("alpha")
    }

    override fun updateUniforms() {
        glUniform1i(getUniform("texture"), 0)
        glUniform2f(getUniform("texelSize"), 
            1f / mc.displayWidth * renderScale,
            1f / mc.displayHeight * renderScale
        )
        glUniform1f(getUniform("radius"), 2f)
        glUniform1f(getUniform("alpha"), 0.6f)
    }

    fun begin(enable: Boolean): FrostShader {
        if (!enable) return this
        
        mc.framebuffer.unbindFramebuffer()
        beginShader()
        return this
    }
} 