/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.minecraft.client.ParticleManager;
import net.minecraft.client.entity.particle.EmitterParticle;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

@Mixin(ParticleManager.class)
@SideOnly(Side.CLIENT)
public abstract class MixinParticleManager {

    @Shadow
    protected abstract void updateEffectLayer(int layer);

    @Shadow
    private List<EmitterParticle> particleEmitters;

    /**
     * @author Mojang
     * @author Marco
     */
    @Overwrite
    public void updateEffects() {
        try {
            for (int i = 0; i < 4; ++i)
                updateEffectLayer(i);

            for (final Iterator<EmitterParticle> it = particleEmitters.iterator(); it.hasNext(); ) {
                final EmitterParticle entityParticleEmitter = it.next();

                entityParticleEmitter.onUpdate();

                if (entityParticleEmitter.isDead)
                    it.remove();
            }
        } catch(final ConcurrentModificationException ignored) {
        }
    }
}