/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.ccbluex.liquidbounce.features.module.modules.render.FreeCam;
import net.ccbluex.liquidbounce.injection.implementations.IMixinEntity;
import net.ccbluex.liquidbounce.utils.client.PacketUtilsKt;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.client.render.Culler;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isPlayerSleeping()Z"))
    private boolean injectFreeCam(LivingEntity instance) {
        return FreeCam.INSTANCE.renderPlayerFromAllPerspectives(instance);
    }

    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;shouldRender(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/Culler;DDD)Z"))
    private boolean injectFreeCamB(EntityRenderDispatcher instance, Entity entity, Culler camera, double x, double y, double z) {
        if (entity instanceof LivingEntity) {
            IMixinEntity iEntity = (IMixinEntity) entity;

            if (iEntity.getTruePos()) {
                PacketUtilsKt.interpolatePosition(iEntity);
            }
        }

        return FreeCam.INSTANCE.handleEvents() || instance.shouldRender(entity, camera, x, y, z);
    }
}
