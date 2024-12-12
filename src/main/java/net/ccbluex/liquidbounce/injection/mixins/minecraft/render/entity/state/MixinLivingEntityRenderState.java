package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.entity.state;

import net.ccbluex.liquidbounce.interfaces.EntityRenderStateAddition;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class MixinLivingEntityRenderState implements EntityRenderStateAddition<LivingEntity> {
    @Unique
    LivingEntity entity;

    @Unique
    public LivingEntity liquid_bounce$getEntity() {
        return entity;
    }

    @Unique
    public void liquid_bounce$setEntity(LivingEntity entity) {
        this.entity = entity;
    }
}
