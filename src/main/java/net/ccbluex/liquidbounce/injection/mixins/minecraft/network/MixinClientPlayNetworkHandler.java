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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.common.ChunkUpdateFlag;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.*;
import net.ccbluex.liquidbounce.features.module.modules.exploit.disabler.disablers.DisablerSpigotSpam;
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.ModuleBetterChat;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAntiExploit;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleNoRotateSet;
import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.kotlin.Priority;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler extends ClientCommonNetworkHandler {

    protected MixinClientPlayNetworkHandler(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
        super(client, connection, connectionState);
    }

    @Inject(method = "onChunkData", at = @At("RETURN"))
    private void injectChunkLoadEvent(ChunkDataS2CPacket packet, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ChunkLoadEvent(packet.getChunkX(), packet.getChunkZ()));
    }

    @Inject(method = "onUnloadChunk", at = @At("RETURN"))
    private void injectUnloadEvent(UnloadChunkS2CPacket packet, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ChunkUnloadEvent(packet.pos().x, packet.pos().z));
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("HEAD"))
    private void onChunkDeltaUpdateStart(ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci) {
        ChunkUpdateFlag.chunkUpdate = true;
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("RETURN"))
    private void onChunkDeltaUpdateEnd(ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci) {
        var chunkPosition = packet.sectionPos.toChunkPos();
        EventManager.INSTANCE.callEvent(new ChunkDeltaUpdateEvent(chunkPosition.x, chunkPosition.z));
        ChunkUpdateFlag.chunkUpdate = false;
    }

    // TODO: test this
    @ModifyExpressionValue(method = "onExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ExplosionS2CPacket;playerKnockback()Ljava/util/Optional;"))
    private Optional<Vec3d> modifyExplosionKnockback(Optional<Vec3d> original) {
        Vec3d orig = original.orElse(Vec3d.ZERO);
        if (ModuleAntiExploit.INSTANCE.getRunning() && ModuleAntiExploit.INSTANCE.getLimitExplosionStrength()) {
            double fixedX = MathHelper.clamp(orig.x, -10.0, 10.0);
            double fixedY = MathHelper.clamp(orig.y, -10.0, 10.0);
            double fixedZ = MathHelper.clamp(orig.z, -10.0, 10.0);

            if (fixedX != orig.x || fixedY != orig.y || fixedZ != orig.z) {
                ModuleAntiExploit.INSTANCE.notifyAboutExploit("Limited too strong explosion",
                        true);
                return Optional.of(new Vec3d(fixedX, fixedY, fixedZ));
            }
        }
        return original;
    }

    @ModifyExpressionValue(method = "onParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ParticleS2CPacket;getCount()I", ordinal = 1))
    private int onParticleAmount(int original) {
        if (ModuleAntiExploit.INSTANCE.getRunning() && ModuleAntiExploit.INSTANCE.getLimitParticlesAmount() && 500 <= original) {
            ModuleAntiExploit.INSTANCE.notifyAboutExploit("Limited too many particles", true);
            return 100;
        }
        return original;
    }

    @ModifyExpressionValue(method = "onParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ParticleS2CPacket;getSpeed()F"))
    private float onParticleSpeed(float original) {
        if (ModuleAntiExploit.INSTANCE.getRunning() && ModuleAntiExploit.INSTANCE.getLimitParticlesSpeed() && 10.0f <= original) {
            ModuleAntiExploit.INSTANCE.notifyAboutExploit("Limited too fast particles speed", true);
            return 10.0f;
        }
        return original;
    }

    // todo: fix this
//    @ModifyExpressionValue(method = "onExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ExplosionS2CPacket;getRadius()F"))
//    private float onExplosionWorld(float original) {
//        if (ModuleAntiExploit.INSTANCE.getRunning() && ModuleAntiExploit.INSTANCE.getLimitExplosionRange()) {
//            float radius = MathHelper.clamp(original, -1000.0f, 1000.0f);
//            if (radius != original) {
//                ModuleAntiExploit.INSTANCE.notifyAboutExploit("Limited too big TNT explosion radius", true);
//                return radius;
//            }
//        }
//        return original;
//    }

    @ModifyExpressionValue(method = "onGameStateChange", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/GameStateChangeS2CPacket;getReason()Lnet/minecraft/network/packet/s2c/play/GameStateChangeS2CPacket$Reason;"))
    private GameStateChangeS2CPacket.Reason onGameStateChange(GameStateChangeS2CPacket.Reason original) {
        if (ModuleAntiExploit.INSTANCE.getRunning() && original == GameStateChangeS2CPacket.DEMO_MESSAGE_SHOWN && ModuleAntiExploit.INSTANCE.getCancelDemo()) {
            ModuleAntiExploit.INSTANCE.notifyAboutExploit("Cancelled demo GUI (just annoying thing)", false);
            return null;
        }

        return original;
    }

    @Inject(method = "onHealthUpdate", at = @At("RETURN"))
    private void injectHealthUpdate(HealthUpdateS2CPacket packet, CallbackInfo ci) {
        ClientPlayerEntity player = this.client.player;

        if (player == null) {
            return;
        }

        EventManager.INSTANCE.callEvent(new HealthUpdateEvent(packet.getHealth(), packet.getFood(), packet.getSaturation(), player.getHealth()));

        if (packet.getHealth() == 0) {
            EventManager.INSTANCE.callEvent(new DeathEvent());
        }
    }

    @Inject(method = "onPlayerPositionLook", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;setPosition(Lnet/minecraft/entity/player/PlayerPosition;Ljava/util/Set;Lnet/minecraft/entity/Entity;Z)Z", shift = At.Shift.AFTER))
    private void injectNoRotateSet(PlayerPositionLookS2CPacket packet, CallbackInfo ci, @Local PlayerEntity playerEntity) {
        if (playerEntity.hasVehicle() ||
                !ModuleNoRotateSet.INSTANCE.getRunning() ||
                MinecraftClient.getInstance().currentScreen instanceof DownloadingTerrainScreen) {
            return;
        }

        // Cancel the original handling
        ci.cancel();

        float yaw = packet.change().yaw();
        float pitch = packet.change().pitch();

        // Confirm teleport
        this.connection.send(new TeleportConfirmC2SPacket(packet.teleportId()));

        // Silently accept yaw and pitch values requested by the server.
        this.connection.send(new PlayerMoveC2SPacket.Full(
                playerEntity.getX(),
                playerEntity.getY(),
                playerEntity.getZ(),
                yaw,
                pitch,
                false, // is always false on 1.21.3
                false // is always false on 1.21.3
        ));

        var activeChoice = ModuleNoRotateSet.INSTANCE.getMode().getActiveChoice();
        if (activeChoice.equals(ModuleNoRotateSet.ResetRotation.INSTANCE)) {
            // Changes your server side rotation and then resets it with provided settings
            var aimPlan = ModuleNoRotateSet.ResetRotation.INSTANCE.getRotationsConfigurable().toAimPlan(
                    new Rotation(yaw, pitch, true),
                    null,
                    null,
                    true,
                    null
            );
            RotationManager.INSTANCE.aimAt(aimPlan, Priority.NOT_IMPORTANT, ModuleNoRotateSet.INSTANCE);
        } else {
            // Increase yaw and pitch by a value so small that the difference cannot be seen,
            // just to update the rotation server-side.
            playerEntity.setYaw(playerEntity.prevYaw + 0.000001f);
            playerEntity.setPitch(playerEntity.prevPitch + 0.000001f);
        }
    }

    @ModifyVariable(method = "sendChatMessage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private String handleSendMessage(String content) {
        var result = ModuleBetterChat.INSTANCE.modifyMessage(content);

        if (DisablerSpigotSpam.INSTANCE.getRunning()) {
            return DisablerSpigotSpam.INSTANCE.getMessage() + " " + result;
        }

        return result;
    }

}
