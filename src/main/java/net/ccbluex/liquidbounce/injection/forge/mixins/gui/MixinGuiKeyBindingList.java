/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.options.ControlsListWidget;
import net.minecraft.client.gui.widget.ListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ControlsListWidget.class)
public abstract class MixinControlsListWidget extends ListWidget {

    public MixinControlsListWidget(Minecraft mcIn, int width, int height, int topIn, int bottomIn, int slotHeightIn) {
        super(mcIn, width, height, topIn, bottomIn, slotHeightIn);
    }

    @Inject(method = "getScrollBarX", at = @At("HEAD"), cancellable = true)
    private void scrollBarX(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(width - 5);
    }
}