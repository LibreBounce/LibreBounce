package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author 00101110001100010111000101111
 * @since 12/18/2024
 **/
@Mixin(ChatScreen.class)
public interface MixinChatScreenAccessor {

    @Accessor("chatField")
    TextFieldWidget getChatField();

}
