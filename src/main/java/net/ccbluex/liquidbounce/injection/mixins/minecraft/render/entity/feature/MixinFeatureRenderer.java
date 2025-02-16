/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.entity.feature;

import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleTrueSight;
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP;
import net.ccbluex.liquidbounce.interfaces.EntityRenderStateAddition;
import net.ccbluex.liquidbounce.render.engine.Color4b;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FeatureRenderer.class)
public abstract class MixinFeatureRenderer {

    @Unique
    private static final int ESP_TRUE_SIGHT_REQUIREMENT_COLOR = new Color4b(255, 255, 255, 255).alpha(120).toARGB();

    @Redirect(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"))
    private static void injectTrueSight(EntityModel instance, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color, @Local(argsOnly = true) LivingEntityRenderState state) {
        final ModuleTrueSight trueSightModule = ModuleTrueSight.INSTANCE;
        final ModuleESP espModule = ModuleESP.INSTANCE;
        final Entity entity = ((EntityRenderStateAddition) state).liquid_bounce$getEntity();
        final LivingEntity livingEntity = entity instanceof LivingEntity ? (LivingEntity) entity : null;
        final boolean trueSight = trueSightModule.getRunning() && trueSightModule.getEntities();
        if (
                (
                        trueSight ||
                                livingEntity != null &&
                                        espModule.getRunning() && espModule.requiresTrueSight(livingEntity)
                ) && entity.isInvisible()
        ) {
            color = trueSight ? trueSightModule.getEntityFeatureLayerColor().toARGB() : ESP_TRUE_SIGHT_REQUIREMENT_COLOR;
        }
        instance.render(matrices, vertices, light, overlay, color);
    }

    @Redirect(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayer;getEntityCutoutNoCull(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"))
    private static RenderLayer injectTrueSight(Identifier texture, @Local(argsOnly = true) LivingEntityRenderState state) {
        final ModuleTrueSight trueSightModule = ModuleTrueSight.INSTANCE;
        final ModuleESP espModule = ModuleESP.INSTANCE;
        final Entity entity = ((EntityRenderStateAddition) state).liquid_bounce$getEntity();
        if (
                (trueSightModule.getRunning() && trueSightModule.getEntities() ||
                        entity instanceof final LivingEntity livingEntity && espModule.getRunning() && espModule.requiresTrueSight(livingEntity)) &&
                        entity.isInvisible()
        ) {
            return RenderLayer.getItemEntityTranslucentCull(texture);
        } else {
            return RenderLayer.getEntityCutoutNoCull(texture);
        }
    }

}
