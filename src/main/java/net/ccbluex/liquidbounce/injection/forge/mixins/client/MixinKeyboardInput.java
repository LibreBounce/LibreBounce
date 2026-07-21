/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.client;

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.InputEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.SuperKnockback;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold;
import net.minecraft.client.entity.living.player.Input;
import net.minecraft.client.entity.living.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput extends MixinInput {

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/living/player/KeyboardInput;jump:Z"))
    private void hookSuperKnockbackInputBlock(CallbackInfo ci) {
        SuperKnockback module = SuperKnockback.INSTANCE;

        if (module.shouldBlockInput()) {
            if (module.getOnlyMove()) {
                this.movementForward = 0f;

                if (!module.getOnlyMoveForward()) {
                    this.movementSideways = 0f;
                }
            }
        }

        Scaffold.INSTANCE.handleMovementOptions(((Input) (Object) this));
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/living/player/KeyboardInput;sneak:Z", ordinal = 1))
    private void injectInputEvent(CallbackInfo ci) {
        EventManager.INSTANCE.call(new InputEvent((Input) (Object) this));
    }
}
