/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.features.module.modules.render.Chat;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {

    @Redirect(method = {"getChatComponent", "drawChat"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/FontRenderer;FONT_HEIGHT:I"))
    private int injectFontChat(FontRenderer instance) {
        return Chat.INSTANCE.shouldModifyChatFont() ? Fonts.fontSemibold40.getHeight() : instance.FONT_HEIGHT;
    }

    @Redirect(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"))
    private int injectFontChatB(FontRenderer instance, String text, float x, float y, int color) {
        return Chat.INSTANCE.shouldModifyChatFont() ? Fonts.fontSemibold40.drawStringWithShadow(text, x, y, color) : instance.drawStringWithShadow(text, x, y, color);
    }

    @Redirect(method = "getChatComponent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I"))
    private int injectFontChatC(FontRenderer instance, String text) {
        return Chat.INSTANCE.shouldModifyChatFont() ? Fonts.fontSemibold40.getStringWidth(text) : instance.getStringWidth(text);
    }

    /**
     * Modifies the message limit constant in the setChatLine method based on ChatControl.
     */
    @ModifyConstant(method = "setChatLine", constant = @Constant(intValue = 100))
    private int fixMsgLimit(int constant) {
        final Chat chat = Chat.INSTANCE;

        if (chat.handleEvents() && chat.getChatClearValue()) {
            return 114514; // Adjust this value as needed
        } else {
            return 100;
        }
    }
}
