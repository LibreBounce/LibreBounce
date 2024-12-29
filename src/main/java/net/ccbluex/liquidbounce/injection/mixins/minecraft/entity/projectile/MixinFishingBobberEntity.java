package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.projectile;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleNoPush;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * MixinFishingBobberEntity
 *
 * @author sqlerrorthing
 * @since 12/28/2024
 **/
@Mixin(FishingBobberEntity.class)
public abstract class MixinFishingBobberEntity {

    @WrapOperation(method = "handleStatus", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/FishingBobberEntity;pullHookedEntity(Lnet/minecraft/entity/Entity;)V"))
    private void hookNoPushByFishingRoad(FishingBobberEntity instance, Entity entity, Operation<Void> original) {
        if (!instance.getWorld().isClient || entity != MinecraftClient.getInstance().player) {
            original.call(instance, entity);
            return;
        }

        if (!ModuleNoPush.INSTANCE.isFishingRoads()) {
            original.call(instance, entity);
        }
    }

}
