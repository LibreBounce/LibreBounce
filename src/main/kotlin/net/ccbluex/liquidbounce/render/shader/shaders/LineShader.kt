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

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.shader.Shader
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.resourceToString
import net.ccbluex.liquidbounce.utils.math.toMat4
import org.lwjgl.opengl.GL30

object LineShader : Shader(
    resourceToString("/assets/liquidbounce/shaders/lines/lines.vert"),
    resourceToString("/assets/liquidbounce/shaders/lines/lines.frag")
) {

    var lineWidth = 1f
    var blendFactor = 1.5f
    var color = Color4b(255, 255, 255, 255)

    private val modelViewMat = UniformPointer("modelViewMat").pointer
    private val projMat = UniformPointer("projMat").pointer
    private val viewPort = UniformPointer("viewPort").pointer
    private val lineWidthLocation = UniformPointer("lineWidth").pointer
    private val blendFactorLocation = UniformPointer("blendFactor").pointer
    private val colorLocation = UniformPointer("color").pointer

    override fun use() {
        RenderSystem.getModelViewMatrix().toMat4().putToUniform(modelViewMat)
        RenderSystem.getProjectionMatrix().toMat4().putToUniform(projMat)
        GL30.glUniform2f(viewPort, mc.window.width.toFloat(), mc.window.height.toFloat())
        GL30.glUniform1f(lineWidthLocation, lineWidth)
        GL30.glUniform1f(blendFactorLocation, blendFactor)
        GL30.glUniform4f(
            colorLocation,
            color.r / 255f,
            color.g / 255f,
            color.b / 255f,
            color.a / 255f
        )
        super.use()
    }

}
