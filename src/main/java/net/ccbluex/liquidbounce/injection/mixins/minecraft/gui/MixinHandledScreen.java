package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleItemScroller;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleInventoryMove;
import net.ccbluex.liquidbounce.utils.client.Chronometer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen<T extends ScreenHandler> extends MixinScreen {

    @Shadow
    private @Nullable Slot touchHoveredSlot;

    @Shadow
    @Final
    protected T handler;

    @Shadow
    @Nullable
    protected abstract Slot getSlotAt(double mouseX, double mouseY);

    @Shadow
    private ItemStack quickMovingStack;

    @Shadow
    protected abstract void onMouseClick(Slot slot, int id, int button, SlotActionType actionType);

    @Shadow
    private boolean cancelNextRelease;

    @Shadow
    private @Nullable Slot lastClickedSlot;

    @Shadow
    private int lastClickedButton;

    @Shadow
    private long lastButtonClickTime;

    @Unique
    @Final
    private final Chronometer chronometer = new Chronometer();

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
    private void cancelMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        var inventoryMove = ModuleInventoryMove.INSTANCE;
        if ((Object) this instanceof InventoryScreen && inventoryMove.getRunning() && inventoryMove.getCancelClicks()) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlots(Lnet/minecraft/client/gui/DrawContext;)V", shift = At.Shift.AFTER))
    private void hookDrawSlot(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!ModuleItemScroller.INSTANCE.getRunning()) {
            return;
        }

        var cursorStack = this.handler.getCursorStack();
        var slot = getSlotAt(mouseX, mouseY);
        var handle = this.client.getWindow().getHandle();

        if (!cursorStack.isEmpty() || slot == null) {
            return;
        }

        if ((InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT)
            || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_SHIFT))
            && GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS
            && chronometer.hasAtLeastElapsed(ModuleItemScroller.getDelay())
        ) {
            this.quickMovingStack = slot.hasStack() ? slot.getStack().copy() : ItemStack.EMPTY;
            this.onMouseClick(slot, slot.id, GLFW.GLFW_MOUSE_BUTTON_LEFT, SlotActionType.QUICK_MOVE);
            this.cancelNextRelease = true;

            this.lastClickedSlot = slot;
            this.lastButtonClickTime = Util.getMeasuringTimeMs();
            this.lastClickedButton = GLFW.GLFW_MOUSE_BUTTON_1;

            chronometer.reset();
        }
    }

}
