/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.ccbluex.liquidbounce.features.misc.FriendManager;
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAntiStaff;
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleBetterTab;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Mixin(PlayerListHud.class)
public abstract class MixinPlayerListHud {

    @Shadow
    protected abstract List<PlayerListEntry> collectPlayerEntries();

    @ModifyConstant(constant = @Constant(longValue = 80L), method = "collectPlayerEntries")
    private long hookTabSize(long count) {
        return ModuleBetterTab.INSTANCE.getRunning() ?
                ModuleBetterTab.Limits.INSTANCE.getTabSize() : count;
    }

    @WrapOperation(method = "collectPlayerEntries", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;sorted(Ljava/util/Comparator;)Ljava/util/stream/Stream;"))
    private Stream<PlayerListEntry> hookSort(Stream<PlayerListEntry> instance, Comparator<PlayerListEntry> defaultComparator, Operation<Stream<PlayerListEntry>> original) {
        var sorting = ModuleBetterTab.INSTANCE.getSorting();

        var betterTab = ModuleBetterTab.INSTANCE;
        var running = betterTab.getRunning();
        var customComparator = sorting.getComparator();

        var comparator = running
                ? (customComparator != null ? customComparator : defaultComparator)
                : defaultComparator;

        var playerHider = ModuleBetterTab.PlayerHider.INSTANCE;
        var hided = running && playerHider.getRunning()
                ? instance.filter(player -> !playerHider.getFilter().isInFilter(player)) : instance;

        return original.call(hided, comparator);
    }

    @ModifyExpressionValue(method = "render", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/hud/PlayerListHud;header:Lnet/minecraft/text/Text;",
            ordinal = 0
    ))
    private Text hookHeader(Text original) {
        if (!ModuleBetterTab.INSTANCE.getRunning()) {
            return original;
        }

        return ModuleBetterTab.Visibility.INSTANCE.getHeader() ?
                original : null;
    }

    @ModifyExpressionValue(method = "render", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/hud/PlayerListHud;footer:Lnet/minecraft/text/Text;",
            ordinal = 0
    ))
    private Text hookFooter(Text original) {
        if (!ModuleBetterTab.INSTANCE.getRunning()) {
            return original;
        }

        return ModuleBetterTab.Visibility.INSTANCE.getFooter() ?
                original : null;
    }

    @ModifyExpressionValue(method = "render", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/hud/PlayerListHud$ScoreDisplayEntry;name:Lnet/minecraft/text/Text;"
    ))
    private Text hookVisibilityName(Text original, @Local(ordinal = 0) PlayerListEntry entry) {
        if (!ModuleBetterTab.INSTANCE.getRunning()) {
            return original;
        }

        return ModuleBetterTab.Visibility.INSTANCE.getNameOnly() ?
                Text.of(entry.getProfile().getName()) : original;

    }

    @ModifyExpressionValue(method = "render", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/PlayerListHud;getPlayerName(Lnet/minecraft/client/network/PlayerListEntry;)Lnet/minecraft/text/Text;"
    ))
    private Text hookWidthVisibilityName(Text original, @Local(ordinal = 0) PlayerListEntry entry) {
        if (!ModuleBetterTab.INSTANCE.getRunning()) {
            return original;
        }

        return ModuleBetterTab.Visibility.INSTANCE.getNameOnly() ?
                Text.of(entry.getProfile().getName()) : original;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", shift = At.Shift.BEFORE))
    private void hookTabColumnHeight(CallbackInfo ci, @Local(ordinal = 5) LocalIntRef o, @Local(ordinal = 6)LocalIntRef p) {
        if (!ModuleBetterTab.INSTANCE.getRunning()) {
            return;
        }

        int totalPlayers = collectPlayerEntries().size();
        int columns = 1;
        int rows = totalPlayers;

        while (rows > ModuleBetterTab.Limits.INSTANCE.getHeight()) {
            columns++;
            rows = (totalPlayers + columns - 1) / columns;
        }

        o.set(rows);
        p.set(columns);
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"), index = 0)
    private int hookWidth(int width) {
        return ModuleBetterTab.INSTANCE.getRunning() && ModuleBetterTab.AccurateLatency.INSTANCE.getRunning() ? width + 30 : width;
    }

    @Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true)
    private void hookOnRenderLatencyIcon(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        var accurateLatency = ModuleBetterTab.AccurateLatency.INSTANCE;
        if (ModuleBetterTab.INSTANCE.getRunning() && accurateLatency.getRunning()) {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

            int latency = MathHelper.clamp(entry.getLatency(), 0, 9999);
            int color = latency < 150 ? 0x00E970 : latency < 300 ? 0xE7D020 : 0xD74238;
            String text = latency + (accurateLatency.getSuffix() ? "ms" : "");
            context.drawTextWithShadow(textRenderer, text, x + width - textRenderer.getWidth(text), y, color);
            ci.cancel();
        }
    }

    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 2))
    private void hookRenderPlayerBackground(Args args, @Local(ordinal = 13) int w, @Local(ordinal = 0) List<PlayerListEntry> entries) {
        if (!ModuleBetterTab.INSTANCE.getRunning()) {
            return;
        }

        var highlight = ModuleBetterTab.Highlight.INSTANCE;
        if (!highlight.getRunning()) {
            return;
        }

        if (w < entries.size()) {
            var entry = entries.get(w);
            if (highlight.getSelf().getRunning()) {
                if (Objects.equals(entry.getProfile().getName(), MinecraftClient.getInstance().player.getGameProfile().getName())) {
                    args.set(4, highlight.getSelf().getColor().toARGB());
                    return;
                }
            }

            if (highlight.getFriends().getRunning()) {
                if (FriendManager.INSTANCE.isFriend(entry.getProfile().getName())) {
                    args.set(4, highlight.getFriends().getColor().toARGB());
                    return;
                }
            }

            var others = highlight.getOthers();
            if (others.getRunning()) {
                if (others.getFilter().isInFilter(entry)) {
                    args.set(4, others.getColor().toARGB());
                }
            }
        }
    }

    @ModifyReturnValue(method = "getPlayerName", at = @At("RETURN"))
    private Text modifyPlayerName(Text original, PlayerListEntry entry) {
        if (ModuleAntiStaff.UsernameCheck.INSTANCE.shouldShowAsStaffOnTab(entry.getProfile().getName())) {
            return original.copy().append(
                    Text.literal(" - (Staff)")
                            .withColor(Colors.LIGHT_RED)
            );
        }
        return original;
    }

}
