/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.block;

import net.ccbluex.liquidbounce.features.module.modules.render.XRay;
import net.minecraft.block.Block;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.resource.model.BakedModel;
import net.minecraft.util.BlockPos;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockModelRenderer.class)
public class MixinBlockModelRenderer {

    @Inject(method = "renderModelAmbientOcclusion", at = @At("HEAD"), cancellable = true)
    private void renderModelAmbientOcclusion(WorldView p_renderModelAmbientOcclusion_1_, BakedModel p_renderModelAmbientOcclusion_2_, Block p_renderModelAmbientOcclusion_3_, BlockPos p_renderModelAmbientOcclusion_4_, BufferBuilder p_renderModelAmbientOcclusion_5_, boolean p_renderModelAmbientOcclusion_6_, CallbackInfoReturnable<Boolean> cir) {
        final XRay xray = XRay.INSTANCE;

        if (xray.handleEvents()) {
            cir.setReturnValue(xray.getXrayBlocks().contains(p_renderModelAmbientOcclusion_3_));
        }
    }

    @Inject(method = "renderModelStandard", at = @At("HEAD"), cancellable = true)
    private void renderModelStandard(WorldView p_renderModelStandard_1_, BakedModel p_renderModelStandard_2_, Block p_renderModelStandard_3_, BlockPos p_renderModelStandard_4_, BufferBuilder p_renderModelStandard_5_, boolean p_renderModelStandard_6_, CallbackInfoReturnable<Boolean> cir) {
        final XRay xray = XRay.INSTANCE;

        if (xray.handleEvents()) {
            cir.setReturnValue(xray.getXrayBlocks().contains(p_renderModelStandard_3_));
        }
    }
}
