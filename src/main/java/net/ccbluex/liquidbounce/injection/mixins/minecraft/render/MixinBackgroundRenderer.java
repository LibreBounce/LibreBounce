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
import com.mojang.blaze3d.systems.RenderSystem;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCustomAmbience;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BackgroundRenderer.FogType;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;
import java.util.stream.Stream;

@Mixin(BackgroundRenderer.class)
public abstract class MixinBackgroundRenderer {

    @Redirect(method = "getFogModifier", at = @At(value = "INVOKE", target = "Ljava/util/List;stream()Ljava/util/stream/Stream;"))
    private static Stream<BackgroundRenderer.StatusEffectFogModifier> injectAntiBlind(List<BackgroundRenderer.StatusEffectFogModifier> list) {
        return list.stream().filter(modifier -> {
            final var effect = modifier.getStatusEffect();

            final var module = ModuleAntiBlind.INSTANCE;

            if (!module.getRunning()) {
                return true;
            }

            return !((StatusEffects.BLINDNESS == effect && module.getAntiBlind()) ||
                    (StatusEffects.DARKNESS == effect && module.getAntiDarkness()));
        });
    }

    @Inject(method = "applyFog", at = @At(value = "INVOKE", shift = At.Shift.AFTER, ordinal = 0, target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogEnd(F)V", remap = false))
    private static void injectLiquidsFog(Camera camera, FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo info) {
        var antiBlind = ModuleAntiBlind.INSTANCE;
        var customAmbienceFog = ModuleCustomAmbience.Fog.INSTANCE;
        if (!antiBlind.getRunning() || customAmbienceFog.getRunning()) {
            return;
        }

        CameraSubmersionType type = camera.getSubmersionType();
        if (antiBlind.getPowderSnowFog() && type == CameraSubmersionType.POWDER_SNOW) {
            RenderSystem.setShaderFogStart(-8.0F);
            RenderSystem.setShaderFogEnd(viewDistance * 0.5F);
            return;
        }

        if (antiBlind.getLiquidsFog()) {
            // Renders fog same as spectator.
            switch (type) {
                case LAVA -> {
                    RenderSystem.setShaderFogStart(-8.0F);
                    RenderSystem.setShaderFogEnd(viewDistance * 0.5F);
                }

                case WATER -> {
                    RenderSystem.setShaderFogStart(-8.0F);
                    RenderSystem.setShaderFogEnd(viewDistance);
                }
            }
        }
    }

    @Inject(method = "applyFog", at = @At("RETURN"))
    private static void injectFog(Camera camera, FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        ModuleCustomAmbience.Fog.INSTANCE.modifyFog(camera, viewDistance);
    }

    @Inject(method = "applyFogColor", at = @At("RETURN"))
    private static void injectFog(CallbackInfo ci) {
        ModuleCustomAmbience.Fog.INSTANCE.modifyFogColor();
    }

    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clearColor(FFFF)V"))
    private static void injectFog(Args args) {
        ModuleCustomAmbience.Fog.INSTANCE.modifySetColorArgs(args);
    }

}
