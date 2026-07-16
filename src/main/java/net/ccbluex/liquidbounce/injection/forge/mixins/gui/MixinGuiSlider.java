/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.minecraft.client.gui.screen.world.GeneratorOptionSlider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GeneratorOptionSlider.class)
public class MixinGeneratorOptionSlider {

    @Redirect(method = "mouseDragged", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GeneratorOptionSlider;drawTexturedModalRect(IIIIII)V"), require = 2)
    public void cancelRectangleDrawing(GeneratorOptionSlider guiSlider, int x, int y, int textureX, int textureY, int width, int height) {
    }

}
