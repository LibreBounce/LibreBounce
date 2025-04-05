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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleHitbox;
import net.ccbluex.liquidbounce.utils.combat.CombatExtensionsKt;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.state.EntityHitbox;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class MixinEntityRenderDispatcher {
    @Unique
    private static Entity entity;

    @Inject(method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "HEAD"))
    private void getEntity(Entity entity, double x, double y, double z, float tickProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        MixinEntityRenderDispatcher.entity = entity;
    }

	@ModifyVariable(method = "renderHitbox", at = @At("HEAD"), argsOnly = true)
	private static EntityHitbox updateBoundingBox(EntityHitbox value) {
		var moduleHitBox = ModuleHitbox.INSTANCE;
		var box = new Box(
				value.x0(),
				value.y0(),
				value.z0(),
				value.x1(),
				value.y1(),
				value.z1()
		);
		if (moduleHitBox.getRunning() && CombatExtensionsKt.shouldBeAttacked(entity)) {
			box = box.expand(moduleHitBox.getSize());
			return new EntityHitbox(
					box.minX,
					box.minY,
					box.minZ,
					box.maxX,
					box.maxY,
					box.maxZ,
					value.red(),
					value.green(),
					value.blue()
			);
		}
		return value;
	}
}
