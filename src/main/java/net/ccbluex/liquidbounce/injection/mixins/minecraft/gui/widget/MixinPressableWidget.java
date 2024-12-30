/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.integration.theme.ThemeManager;
import net.ccbluex.liquidbounce.render.engine.Color4b;
import net.ccbluex.liquidbounce.utils.math.Easing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PressableWidget.class)
public abstract class MixinPressableWidget extends MixinClickableWidget {

    @Shadow
    public abstract void drawMessage(DrawContext context, TextRenderer textRenderer, int color);

    /**
     * An animation factor that is increased when hovering over the button. It serves
     * for the easing (0.0 - 1.0) value and will be increased and decreased by a [ANIMATION_SPEED] * [delta]
     */
    @Unique
    private float factor;
    @Unique
    private final float ANIMATION_SPEED = 0.5f;

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void renderWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo callbackInfo) {
        if (HideAppearance.INSTANCE.isHidingNow() || HideAppearance.INSTANCE.isDestructed()) {
            return;
        }

        // Update animation factor
        if (!isHovered()) {
            factor = (float) Math.max(0.0, factor - ANIMATION_SPEED * delta);
        } else {
            factor = (float) Math.min(1.0, factor + ANIMATION_SPEED * delta);
        }

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();

        var theme = ThemeManager.INSTANCE.getActiveTheme();
        var GUI_BUTTON_TEXTURE = theme.getTextures().get("button");
        var GUI_BUTTON_DISABLED_TEXTURE = theme.getTextures().get("button_disabled");
        var GUI_BUTTON_HOVER_TEXTURE = theme.getTextures().get("button_hover");

        // If any of the textures are null, we will use the default ones
        if (GUI_BUTTON_TEXTURE == null || GUI_BUTTON_DISABLED_TEXTURE == null || GUI_BUTTON_HOVER_TEXTURE == null) {
            return;
        }

        if (factor < 1) {
            Identifier texture;
            if (active) {
                texture = GUI_BUTTON_TEXTURE.getValue();
            } else {
                texture = GUI_BUTTON_DISABLED_TEXTURE.getValue();
            }

            context.drawTexture(RenderLayer::getGuiTextured, texture, this.getX(), this.getY(), 0, 0, this.getWidth(), this.getHeight(),
                    this.getWidth(), this.getHeight(), new Color4b(1f, 1f, 1f, this.alpha).toARGB());
        }

        if (factor > 0) {
            var a = Easing.QUAD_IN_OUT.transform(factor);
            context.drawTexture(RenderLayer::getGuiTextured, GUI_BUTTON_HOVER_TEXTURE.getValue(), this.getX(), this.getY(), 0, 0, this.getWidth(), this.getHeight(),
                    this.getWidth(), this.getHeight(), new Color4b(1f, 1f, 1f, a * this.alpha).toARGB());
        }

        // Draw the message
        var i = this.active ? 16777215 : 10526880;
        var color = i | MathHelper.ceil(this.alpha * 255.0F) << 24;

        this.drawMessage(context, MinecraftClient.getInstance().textRenderer, color);

        // Cancel the method
        callbackInfo.cancel();
    }

}
