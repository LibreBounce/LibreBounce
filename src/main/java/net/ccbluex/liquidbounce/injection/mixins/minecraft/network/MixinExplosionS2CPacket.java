package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.*;

import java.util.Optional;

@Mixin(ExplosionS2CPacket.class)
public abstract class MixinExplosionS2CPacket {
    @Shadow
    @Final
    @Mutable
    private Optional<Vec3d> playerKnockback;

    @Unique
    public void liquid_bounce$setPlayerKnockback(Optional<Vec3d> playerKnockback) {
        this.playerKnockback = playerKnockback;
    }
}
