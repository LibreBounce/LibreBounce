package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import net.ccbluex.liquidbounce.interfaces.PostEffectPassTextureAddition;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.util.Handle;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(PostEffectPass.class)
public class MixinPostEffectPass implements PostEffectPassTextureAddition {
    @Shadow
    @Final
    private ShaderProgram program;
    @Unique
    private final Map<String, Integer> textureSamplerMap = new HashMap<>();

    @Override
    public void liquid_bounce$setTextureSampler(String name, int textureId) {
        this.textureSamplerMap.put(name, textureId);

    }

    // todo: is `addSamplerTexture` what we need / want?
    // todo: we need to inject at the endWrite invoke in the lambda passed to `renderPass.setRenderer`,
    //  not sure how to do that
//    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;endWrite()V"))
//    private void injectTextureSamplerMap(FrameGraphBuilder builder, Map<Identifier, Handle<Framebuffer>> handles, Matrix4f projectionMatrix, CallbackInfo ci) {
//        for (Map.Entry<String, Integer> stringIntegerEntry : this.textureSamplerMap.entrySet()) {
//            this.program.addSamplerTexture(stringIntegerEntry.getKey(), stringIntegerEntry.getValue());
//        }
//    }

}
