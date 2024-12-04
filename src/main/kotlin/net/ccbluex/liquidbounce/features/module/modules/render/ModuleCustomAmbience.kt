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
package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.MixinBackgroundRenderer
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.minecraft.block.enums.CameraSubmersionType
import net.minecraft.client.render.BackgroundRenderer.FogType
import net.minecraft.client.render.Camera
import net.minecraft.client.render.FogShape
import net.minecraft.util.math.MathHelper

/**
 * CustomAmbience module
 *
 * Override the ambience of the game
 */
object ModuleCustomAmbience : ClientModule("CustomAmbience", Category.RENDER) {

    val weather = enumChoice("Weather", WeatherType.SUNNY)
    private val time = enumChoice("Time", TimeType.NOON)

    object Precipitation : ToggleableConfigurable(this, "ModifyPrecipitation", false) {
        val gradient by float("Gradient", 1f, 0.1f..1f)
        val layers by int("Layers", 7, 1..15)
    }

    object Fog : ToggleableConfigurable(this, "Fog", false) {

        private val color by color("Color", Color4b(70, 119, 255, 255))
        private val fogStart by float("FogStart", 0.25f, -8f..2000f)
        private val fogEnd by float("FogEnd", 1f, 0f..2000f)
        private val fogShape by enumChoice("FogShape", Shape.CYLINDER)

        /**
         * [MixinBackgroundRenderer]
         */
        fun modifyFog(camera: Camera, fogType: FogType, viewDistance: Float) {
            if (!this.running || fogType != FogType.FOG_TERRAIN) {
                return
            }

            RenderSystem.setShaderFogStart(MathHelper.clamp(fogStart, -8f, viewDistance))
            RenderSystem.setShaderFogEnd(MathHelper.clamp(fogEnd, 0f, viewDistance))

            val type = camera.submersionType
            if (type != CameraSubmersionType.NONE) {
                return
            }

            RenderSystem.setShaderFogShape(fogShape.fogShape)

            val color = color
            RenderSystem.setShaderFogColor(
                color.r / 255f,
                color.g / 255f,
                color.b / 255f,
                color.a / 255f
            )
        }

        @Suppress("unused")
        private enum class Shape(override val choiceName: String, val fogShape: FogShape) : NamedChoice {
            SPHERE("Sphere", FogShape.SPHERE),
            CYLINDER("Cylinder", FogShape.CYLINDER);
        }

    }

    object CustomLightColor : ToggleableConfigurable(this, "CustomLightColor", false) {

        private val lightColor by color("LightColor", Color4b(70, 119, 255, 255))

        fun blendWithLightColor(srcColor: Int): Int {
            if (lightColor.a == 255) {
                return lightColor.toABGR()
            } else if (lightColor.a == 0) {
                return srcColor
            }

            val srcB = (srcColor shr 16) and 0xFF
            val srcG = (srcColor shr 8) and 0xFF
            val srcR = srcColor and 0xFF

            val dstAlpha = lightColor.a / 255f

            val outB = ((srcB * (1 - dstAlpha)) + (lightColor.b * dstAlpha)).toInt()
            val outG = ((srcG * (1 - dstAlpha)) + (lightColor.g * dstAlpha)).toInt()
            val outR = ((srcR * (1 - dstAlpha)) + (lightColor.r * dstAlpha)).toInt()

            return (255 shl 24) or (outB shl 16) or (outG shl 8) or outR
        }

    }

    init {
        tree(Precipitation)
        tree(Fog)
        tree(CustomLightColor)
    }

    @JvmStatic
    fun getTime(original: Long): Long {
        return if (running) {
            when (time.get()) {
                TimeType.NO_CHANGE -> original
                TimeType.DAWN -> 23041L
                TimeType.DAY -> 1000L
                TimeType.NOON -> 6000L
                TimeType.DUSK -> 12610L
                TimeType.NIGHT -> 13000L
                TimeType.MID_NIGHT -> 18000L
            }
        } else {
            original
        }
    }

    @Suppress("unused")
    enum class WeatherType(override val choiceName: String) : NamedChoice {
        NO_CHANGE("NoChange"),
        SUNNY("Sunny"),
        RAINY("Rainy"),
        SNOWY("Snowy"),
        THUNDER("Thunder")
    }

    enum class TimeType(override val choiceName: String) : NamedChoice {
        NO_CHANGE("NoChange"),
        DAWN("Dawn"),
        DAY("Day"),
        NOON("Noon"),
        DUSK("Dusk"),
        NIGHT("Night"),
        MID_NIGHT("MidNight")
    }

}
