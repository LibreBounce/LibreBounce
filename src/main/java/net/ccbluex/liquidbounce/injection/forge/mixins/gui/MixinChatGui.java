/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.features.module.modules.render.Chat;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.gui.chat.ChatGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ChatGui.class)
public abstract class MixinChatGui {

    @Redirect(method = {"getChatComponent", "drawChat"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/TextRenderer;FONT_HEIGHT:I"))
    private int injectFontChat(TextRenderer instance) {
        return Chat.INSTANCE.handleEvents() ? Chat.INSTANCE.getFont().FONT_HEIGHT : instance.FONT_HEIGHT;
    }

    @Redirect(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/TextRenderer;drawWithShadow(Ljava/lang/String;FFI)I"))
    private int injectFontChatB(TextRenderer instance, String text, float x, float y, int color) {
        final Chat chat = Chat.INSTANCE;

        return chat.handleEvents() ? chat.getFont().draw(text, x, y, color, chat.getTextShadow()) : instance.drawWithShadow(text, x, y, color);
    }

    @Redirect(method = "getChatComponent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/TextRenderer;getWidth(Ljava/lang/String;)I"))
    private int injectFontChatC(TextRenderer instance, String text) {
        return Chat.INSTANCE.handleEvents() ? Chat.INSTANCE.getFont().getWidth(text) : instance.getWidth(text);
    }

    /**
     * Modifies the message limit constant in the setChatLine method based on the Chat module.
     */
    @ModifyConstant(method = "getVisibleLineCount", constant = @Constant(intValue = 100))
    private int fixMsgLimit(int constant) {
        final Chat chat = Chat.INSTANCE;

        if (chat.handleEvents() && chat.getNoMessageLimitValue()) {
            return 114514; // Adjust this value as needed
        } else {
            return 100;
        }
    }
}
