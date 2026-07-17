/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar;
import net.minecraft.entity.living.player.PlayerInventory;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static net.ccbluex.liquidbounce.utils.client.MinecraftInstance.mc;

@Mixin(PlayerInventory.class)
public class MixinPlayerInventory {

    @Redirect(method = {"getCurrentItem", "decrementAnimations", "getStrVsBlock", "canHeldItemHarvest"}, at = @At(value = "FIELD", target = "Lnet/minecraft/entity/living/player/PlayerInventory;currentItem:I", opcode = Opcodes.GETFIELD))
    private int hookSilentHotbar(PlayerInventory instance) {
        if (instance == null || instance.player == null || mc.thePlayer == null)
            return instance != null ? instance.currentItem : 0;

        return instance.player.getGameProfile().equals(mc.thePlayer.getGameProfile()) ? SilentHotbar.INSTANCE.getCurrentSlot() : instance.currentItem;
    }
}
