package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.projectile;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleExtendedFirework;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FireworkRocketEntity.class)
public class MixinFireworkRocketEntity {
    @Shadow
    private LivingEntity shooter;

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;add(DDD)Lnet/minecraft/util/math/Vec3d;", ordinal = 0))
    private Vec3d hookExtendedFirework(Vec3d instance, double x, double y, double z, Operation<Vec3d> original, @Local(ordinal = 0) Vec3d rotation, @Local(ordinal = 1) Vec3d velocity) {
        if (shooter != MinecraftClient.getInstance().player
            || !ModuleExtendedFirework.INSTANCE.getRunning()
        ) {
            return original.call(instance, x, y, z);
        }

        var multiplier = ModuleExtendedFirework.getVelocityMultiplier();

        return instance.add(
                rotation.x * multiplier.x + (rotation.x * multiplier.y - velocity.x) * multiplier.z,
                rotation.y * multiplier.x + (rotation.y * multiplier.y - velocity.y) * multiplier.z,
                rotation.z * multiplier.x + (rotation.z * multiplier.y - velocity.z) * multiplier.z
        );
    }
}
