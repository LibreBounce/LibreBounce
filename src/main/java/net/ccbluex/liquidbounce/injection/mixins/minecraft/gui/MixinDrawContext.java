package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.ccbluex.liquidbounce.interfaces.DrawContextAddition;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.GuiAtlasManager;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Function;

@Mixin(DrawContext.class)
public abstract class MixinDrawContext implements DrawContextAddition {

    @Shadow
    @Final
    private MatrixStack matrices;

    @Shadow
    @Final
    private VertexConsumerProvider.Immediate vertexConsumers;

    @Shadow
    @Final
    private GuiAtlasManager guiAtlasManager;

    @Override
    public void liquid_bounce$drawTexture(Function<Identifier, RenderLayer> renderLayers, Identifier texture, float x, float y, int width, int height) {
        Sprite sprite = guiAtlasManager.getSprite(texture);
        float o = 1 / 32768f;
        liquid_bounce$drawTexturedQuad(renderLayers, sprite.getAtlasId(), x, x + width, y, y + height, sprite.getMinU() + o, sprite.getMaxU() + o, sprite.getMinV() - o, sprite.getMaxV() - o, -1);
    }

    @Override
    public void liquid_bounce$drawTexturedQuad(Function<Identifier, RenderLayer> renderLayers, Identifier texture, float x1, float x2, float y1, float y2, float u1, float u2, float v1, float v2, int color) {
        RenderLayer renderLayer = renderLayers.apply(texture);
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        VertexConsumer bufferBuilder = vertexConsumers.getBuffer(renderLayer);
        bufferBuilder.vertex(matrix4f, x1, y1, 0f).texture(u1, v1).color(color);
        bufferBuilder.vertex(matrix4f, x1, y2, 0f).texture(u1, v2).color(color);
        bufferBuilder.vertex(matrix4f, x2, y2, 0f).texture(u2, v2).color(color);
        bufferBuilder.vertex(matrix4f, x2, y1, 0f).texture(u2, v1).color(color);
    }
}
