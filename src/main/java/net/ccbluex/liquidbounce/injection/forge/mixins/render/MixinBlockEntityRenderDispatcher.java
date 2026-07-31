/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.ccbluex.liquidbounce.features.module.modules.render.XRay;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class MixinBlockEntityRenderDispatcher {

    @Inject(method = "renderBlockEntity", at = @At("HEAD"), cancellable = true)
    private void renderBlockEntity(BlockEntity p_renderBlockEntity_1_, float p_renderBlockEntity_2_, int p_renderBlockEntity_3_, CallbackInfo ci) {
        final XRay xray = XRay.INSTANCE;

        if (xray.handleEvents() && !xray.getXrayBlocks().contains(p_renderBlockEntity_1_.getBlockType())) {
            ci.cancel();
        }
    }
}
