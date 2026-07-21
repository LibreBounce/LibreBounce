/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.tweaks;

import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.chunk.storage.AnvilChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.DataInputStream;
import java.io.IOException;

@Mixin(AnvilChunkStorage.class)
public class MixinAnvilChunkStorage {

    /**
     * Due to limitations with local variable capture in Mixins,
     * a Redirect is used as an alternative.
     */
    @Redirect(
            method = "loadChunk__Async",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/NbtIo;read(Ljava/io/DataInputStream;)Lnet/minecraft/nbt/NbtCompound;"
            )
    )

    private NbtCompound redirectReadChunkData(DataInputStream inputStream) throws IOException {
        try (DataInputStream stream = inputStream) {
            return NbtIo.read(stream);
        }
    }
}
