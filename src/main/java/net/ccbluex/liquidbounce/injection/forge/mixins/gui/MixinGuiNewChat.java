/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.features.module.modules.misc.ChatControl;
import net.ccbluex.liquidbounce.features.module.modules.render.HUD;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.ccbluex.liquidbounce.utils.client.MinecraftInstance.mc;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {

    private final Map<String, Integer> messageCounts = new HashMap<>();

    @Redirect(method = {"getChatComponent", "drawChat"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/FontRenderer;FONT_HEIGHT:I"))
    private int injectFontChat(FontRenderer instance) {
        return HUD.INSTANCE.shouldModifyChatFont() ? Fonts.font40.getHeight() : instance.FONT_HEIGHT;
    }

    @Redirect(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"))
    private int injectFontChatB(FontRenderer instance, String text, float x, float y, int color) {
        return HUD.INSTANCE.shouldModifyChatFont() ? Fonts.font40.drawStringWithShadow(text, x, y, color) : instance.drawStringWithShadow(text, x, y, color);
    }

    @Redirect(method = "getChatComponent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I"))
    private int injectFontChatC(FontRenderer instance, String text) {
        return HUD.INSTANCE.shouldModifyChatFont() ? Fonts.font40.getStringWidth(text) : instance.getStringWidth(text);
    }

    @Inject(method = "printChatMessage", at = @At("HEAD"), cancellable = true)
    public void onPrintChatMessage(IChatComponent chatComponent, CallbackInfo ci) {
        String rawMessage = chatComponent.getFormattedText(); //.getUnformattedText().trim();
        String messageId = String.valueOf(rawMessage.hashCode());

        if (ChatControl.INSTANCE.handleEvents() && ChatControl.INSTANCE.getStackMessage()) {
            int count = messageCounts.getOrDefault(messageId, 0) + 1;
            messageCounts.put(messageId, count);

            if (count > 1) {
                String modifiedMessage = rawMessage + " " + EnumChatFormatting.GRAY + "[" + count + "x]";
                ChatComponentText stackedComponent = new ChatComponentText(modifiedMessage);

                ci.cancel();
                mc.ingameGUI.getChatGUI().printChatMessage(stackedComponent);
            }

            if (messageCounts.size() > 100) {
                String firstKey = messageCounts.keySet().iterator().next();
                messageCounts.remove(firstKey);
            }
        }
    }

    @Redirect(method = "setChatLine", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 0))
    private int hookNoLengthLimit(List<ChatLine> list) {
        ChatControl chatControl = ChatControl.INSTANCE;

        if (chatControl.handleEvents() && chatControl.getNoLengthLimit()) {
            return -1;
        }

        return list.size();
    }

    @Inject(method = "clearChatMessages", at = @At("HEAD"), cancellable = true)
    private void hookChatClear(CallbackInfo ci) {
        final ChatControl chatControl = ChatControl.INSTANCE;

        if (chatControl.handleEvents() && chatControl.getNoChatClear()) {
            ci.cancel();
        }
    }
}
