package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.PaintingEntity;
import net.minecraft.block.spawner.MobSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.render.block.entity.MobSpawnerRenderer.class)
public class MixinMobSpawnerRenderer {

    @Inject(method = "renderMob", cancellable = true, at = @At("HEAD"))
    private static void injectPaintingSpawnerFix(MobSpawner mobSpawnerLogic, double posX, double posY, double posZ, float partialTicks, CallbackInfo ci) {
        Entity entity = mobSpawnerLogic.func_180612_a(mobSpawnerLogic.getSpawnerWorld());

        if (entity == null || entity instanceof PaintingEntity)
            ci.cancel();
    }

}
