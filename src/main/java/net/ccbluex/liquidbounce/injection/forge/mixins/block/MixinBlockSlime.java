/*
 * SkidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge, Forked from LiquidBounce.
 * https://github.com/ManInMyVan/SkidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.block;

import net.ccbluex.liquidbounce.features.module.modules.movement.AntiBounce;
import net.minecraft.block.BlockSlime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class MixinBlockSlime {
    @Inject(method = "onLanded", at = @At("HEAD"), cancellable = true)
    private void AntiBounce(CallbackInfo callbackInfo) {
        if (AntiBounce.INSTANCE.handleEvents()) {
            callbackInfo.cancel();
        }
    }
}
