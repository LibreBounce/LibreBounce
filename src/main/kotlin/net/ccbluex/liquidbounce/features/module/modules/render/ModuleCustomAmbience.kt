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
import net.ccbluex.liquidbounce.utils.client.FloatColor
import net.ccbluex.liquidbounce.utils.client.copy
import net.minecraft.block.enums.CameraSubmersionType
import net.minecraft.client.render.Camera
import net.minecraft.client.render.FogShape
import net.minecraft.util.math.MathHelper
import org.spongepowered.asm.mixin.injection.invoke.arg.Args

/**
 * CustomAmbience module
 *
 * Override the ambience of the game
 */
object ModuleCustomAmbience : ClientModule("CustomAmbience", Category.RENDER) {

    val weather = enumChoice("Weather", WeatherType.SNOWY)
    private val time = enumChoice("Time", TimeType.NIGHT)

    object Precipitation : ToggleableConfigurable(this, "ModifyPrecipitation", true) {
        val gradient by float("Gradient", 0.7f, 0.1f..1f)
        val layers by int("Layers", 3, 1..14)
    }

    object Fog : ToggleableConfigurable(this, "Fog", true) {

        private val color by color("Color", Color4b(47, 128, 255, 201))
        private val backgroundColor by color("BackgroundColor", Color4b(47, 128, 255, 201))
        private val fogStart by float("Distance", 0f, -8f..500f)
        private val density by float("Density", 10f, 0f..100f)
        private val fogShape by enumChoice("FogShape", Shape.SPHERE)

        /**
         * [MixinBackgroundRenderer]
         */
        fun modifyFog(camera: Camera, viewDistance: Float, original: net.minecraft.client.render.Fog) {
            if (!this.running) {
                return
            }

            var newFog = original.copy(
                positionPair = Pair(
                    MathHelper.clamp(fogStart, -8f, viewDistance),
                    MathHelper.clamp(fogStart + density, 0f, viewDistance)
                ),
            )

            val type = camera.submersionType
            if (type != CameraSubmersionType.NONE) {
                return
            }

            newFog.copy(shape = fogShape.fogShape)

            val color = color
            newFog.copy(
                color = FloatColor(
                    color.r / 255f,
                    color.g / 255f,
                    color.b / 255f,
                    color.a / 255f
                )
            )
        }

        fun modifySetColorArgs(args: Args) {
            if (!this.running || backgroundColor.a == 0) {
                return
            }

            args.setAll(backgroundColor.r / 255f, backgroundColor.g / 255f, backgroundColor.b / 255f, 0f)
        }

        @Suppress("unused")
        private enum class Shape(override val choiceName: String, val fogShape: FogShape) : NamedChoice {
            SPHERE("Sphere", FogShape.SPHERE),
            CYLINDER("Cylinder", FogShape.CYLINDER);
        }

    }

    object CustomLightColor : ToggleableConfigurable(this, "CustomLightColor", true) {

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
