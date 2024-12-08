/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.packets;

import net.ccbluex.liquidbounce.features.module.modules.misc.ChatControl;
import net.minecraft.network.play.client.C01PacketChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(C01PacketChatMessage.class)
public class MixinC01PacketChatMessage {

    @Shadow
    public String message;

    @Inject(method = "<init>(Ljava/lang/String;)V", at = @At("RETURN"), cancellable = true)
    public void injectForceUnicodeChat(String p_i45240_1_, CallbackInfo ci) {
        if (ChatControl.INSTANCE.handleEvents() && ChatControl.INSTANCE.getForceUnicodeChat()) {
            if (p_i45240_1_.startsWith("/")) {
                return;
            }

            StringBuilder stringBuilder = new StringBuilder();

            for (char c : p_i45240_1_.toCharArray()) {
                if (c >= 33 && c <= 128) {
                    stringBuilder.append(Character.toChars(c + 65248));
                } else {
                    stringBuilder.append(c);
                }
            }

            this.message = stringBuilder.toString();
        }

        ci.cancel();
    }
}