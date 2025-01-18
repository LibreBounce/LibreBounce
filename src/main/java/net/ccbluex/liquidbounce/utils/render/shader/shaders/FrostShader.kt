package net.ccbluex.liquidbounce.utils.render.shader.shaders

import net.ccbluex.liquidbounce.utils.render.shader.FramebufferShader
import org.lwjgl.opengl.GL20.*
import java.io.Closeable
import java.awt.Color

object FrostShader : FramebufferShader("frost.frag"), Closeable {
    var isInUse = false
        private set
    
    var intensity = 0.3f
    var tintColor = Color.WHITE
    
    override fun setupUniforms() {
        setupUniform("texture")
        setupUniform("texelSize")
        setupUniform("radius")
        setupUniform("alpha")
        setupUniform("intensity")
        setupUniform("tintColor")
    }

    override fun updateUniforms() {
        glUniform1i(getUniform("texture"), 0)
        glUniform2f(getUniform("texelSize"), 
            1f / mc.displayWidth * renderScale,
            1f / mc.displayHeight * renderScale
        )
        glUniform1f(getUniform("radius"), 2f)
        glUniform1f(getUniform("alpha"), 0.6f)
        glUniform1f(getUniform("intensity"), intensity)
        glUniform3f(getUniform("tintColor"), 
            tintColor.red / 255f,
            tintColor.green / 255f,
            tintColor.blue / 255f
        )
    }

    override fun startShader() {
        super.startShader()
        isInUse = true
    }

    override fun stopShader() {
        super.stopShader()
        isInUse = false
    }

    override fun close() {
        if (isInUse)
            stopShader()
    }

    fun begin(enable: Boolean, intensity: Float = 0.3f, tintColor: Color = Color.WHITE) = apply {
        if (!enable) return@apply
        this.intensity = intensity
        this.tintColor = tintColor
        startShader()
    }
} 