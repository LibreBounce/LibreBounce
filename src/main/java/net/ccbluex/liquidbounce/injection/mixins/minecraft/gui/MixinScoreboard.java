package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes a ViaFabricPlus bug where it would not correctly handle the scoreboard data, resulting in a disconnect.
 * <p>
 * ViaFabricPlus versions: 3.4.8-?
 */
@Mixin(Scoreboard.class)
public abstract class MixinScoreboard {

    @Shadow
    @Nullable
    public abstract Team getScoreHolderTeam(String scoreHolderName);


    @ModifyExpressionValue(method = "addObjective", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/Object2ObjectMap;containsKey(Ljava/lang/Object;)Z", remap = false))
    private boolean noCrash(boolean original) {
        return false;
    }

    @Inject(method = "removeScoreHolderFromTeam", at = @At("HEAD"), cancellable = true)
    private void noCrash2(String scoreHolderName, Team team, CallbackInfo ci) {
        if (getScoreHolderTeam(scoreHolderName) != team) {
            ci.cancel();
        }
    }

}
