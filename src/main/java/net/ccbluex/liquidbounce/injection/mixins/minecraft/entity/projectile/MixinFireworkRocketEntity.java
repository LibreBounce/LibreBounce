package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.projectile;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FireworkRocketEntity.class)
public abstract class MixinFireworkRocketEntity {
    @Shadow
    private LivingEntity shooter;

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d getRotationVector(Vec3d original) {
        if (shooter != MinecraftClient.getInstance().player) {
            return original;
        }

        var rotation = RotationManager.getCurrentRotation();
        var configurable = RotationManager.getActiveRotationTarget();

        if (rotation == null || configurable == null || configurable.getMovementCorrection() == MovementCorrection.OFF) {
            return original;
        }

        return rotation.getDirectionVector();
    }
}
