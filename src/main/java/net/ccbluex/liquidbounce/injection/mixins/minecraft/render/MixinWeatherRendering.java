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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.features.module.modules.render.*;
import net.minecraft.client.render.*;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(WeatherRendering.class)
public abstract class MixinWeatherRendering {

    @ModifyExpressionValue(method = "buildPrecipitationPieces", at = @At(value = "INVOKE",  target = "Lnet/minecraft/client/render/WeatherRendering;getPrecipitationAt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/biome/Biome$Precipitation;"))
    private Biome.Precipitation modifyBiomePrecipitation(
            Biome.Precipitation original) {
        var moduleOverrideWeather = ModuleCustomAmbience.INSTANCE;

        if (!moduleOverrideWeather.getRunning())
            return original;

        return switch (moduleOverrideWeather.getWeather().get()) {
            case RAINY -> Biome.Precipitation.RAIN;
            case SNOWY -> Biome.Precipitation.SNOW;
            default -> original;
        };

    }

}
