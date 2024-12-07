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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCustomAmbience;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.entity.effect.StatusEffects;
import org.joml.Vector4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

//    @Inject(method = "applyFog", at = @At(
//            value = "FIELD", opcode = Opcodes.PUTFIELD,
//            target = "Lnet/minecraft/client/render/BackgroundRenderer$FogData;fogStart:F", remap = false))
//    private static void injectLiquidsFog(Camera camera, BackgroundRenderer.FogType fogType, Vector4f color, float viewDistance, boolean thickenFog, float tickDelta, CallbackInfoReturnable<Fog> cir) {
//        var antiBlind = ModuleAntiBlind.INSTANCE;
//        var customAmbienceFog = ModuleCustomAmbience.Fog.INSTANCE;
//        if (!antiBlind.getRunning() || customAmbienceFog.getRunning()) {
//            return;
//        }
//
//        CameraSubmersionType type = camera.getSubmersionType();
//        if (antiBlind.getPowderSnowFog() && type == CameraSubmersionType.POWDER_SNOW) {
//            RenderSystem.setShaderFogStart(-8.0F);
//            RenderSystem.setShaderFogEnd(viewDistance * 0.5F);
//            return;
//        }
//
//        if (antiBlind.getLiquidsFog()) {
//            // Renders fog same as spectator.
//            switch (type) {
//                case LAVA -> {
//                    RenderSystem.setShaderFogStart(-8.0F);
//                    RenderSystem.setShaderFogEnd(viewDistance * 0.5F);
//                }
//
//                case WATER -> {
//                    RenderSystem.setShaderFogStart(-8.0F);
//                    RenderSystem.setShaderFogEnd(viewDistance);
//                }
//            }
//        }
//    }
    @WrapOperation(method = "applyFog", at = @At(
            value = "FIELD", opcode = Opcodes.PUTFIELD,
            target = "Lnet/minecraft/client/render/BackgroundRenderer$FogData;fogStart:F", remap = false))
    private static void injectLiquidsFog(BackgroundRenderer.FogData instance, float value, Operation<Void> original) {
        
    }

    @Inject(method = "applyFog", at = @At("RETURN"))
    private static void injectFog(Camera camera, BackgroundRenderer.FogType fogType, Vector4f color, float viewDistance, boolean thickenFog, float tickDelta, CallbackInfoReturnable<Fog> cir) {
        ModuleCustomAmbience.Fog.INSTANCE.modifyFog(camera, fogType, viewDistance, cir.getReturnValue());
    }
}
