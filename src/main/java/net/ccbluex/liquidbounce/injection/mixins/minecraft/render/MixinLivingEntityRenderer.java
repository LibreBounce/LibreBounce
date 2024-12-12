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
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import it.unimi.dsi.fastutil.floats.FloatFloatPair;
import net.ccbluex.liquidbounce.features.cosmetic.CosmeticCategory;
import net.ccbluex.liquidbounce.features.cosmetic.CosmeticService;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleESP;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleRotations;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleTrueSight;
import net.ccbluex.liquidbounce.interfaces.EntityRenderStateAddition;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.apache.commons.lang3.function.Suppliers;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @Unique
    private final ThreadLocal<@Nullable FloatFloatPair> rotationPitch = ThreadLocal.withInitial(Suppliers.nul());

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void injectRender(S livingEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        final var rotationPitch = ModuleRotations.INSTANCE.getRotationPitch();

        this.rotationPitch.remove();

        // todo: well. how do we get the player here?
//        if (livingEntityRenderState != MinecraftClient.getInstance().player) {
//            return;
//        }

        if (!ModuleRotations.INSTANCE.shouldDisplayRotations() || !ModuleRotations.INSTANCE.getBodyParts().getHead()) {
            return;
        }

        this.rotationPitch.set(FloatFloatPair.of(rotationPitch.keyFloat(), rotationPitch.valueFloat()));
    }

    // todo: fix this
//    /**
//     * Head rotation pitch injection hook
//     */
//    @Redirect(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F", ordinal = 0))
//    private float injectRotationPitch(float g, float f, float s) {
//        final var rot = this.rotationPitch.get();
//        if (rot != null) {
//            return MathHelper.lerp(g, rot.keyFloat(), rot.valueFloat());
//        } else {
//            return MathHelper.lerp(g, f, s);
//        }
//    }


    @ModifyExpressionValue(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;isVisible(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;)Z"))
    private boolean injectTrueSight(boolean original, S state) {
        LivingEntity entity = ((EntityRenderStateAddition<LivingEntity>)state).liquid_bounce$getEntity();
        // Check if TrueSight is enabled and entities are enabled or ESP is enabled and in glow mode
        if (ModuleTrueSight.INSTANCE.getRunning() && ModuleTrueSight.INSTANCE.getEntities() ||
                ModuleESP.INSTANCE.getRunning() && ModuleESP.INSTANCE.requiresTrueSight(entity)) {
            return false;
        }

        return original;
    }

    @ModifyReturnValue(method = "shouldFlipUpsideDown", at = @At("RETURN"))
    private static boolean injectShouldFlipUpsideDown(boolean original, LivingEntity entity) {
        if (!(entity instanceof AbstractClientPlayerEntity)) {
            return original;
        }

        return CosmeticService.INSTANCE.hasCosmetic(entity.getUuid(), CosmeticCategory.DINNERBONE);
    }

}
