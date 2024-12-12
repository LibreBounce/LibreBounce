package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.entity.state;

import net.ccbluex.liquidbounce.interfaces.EntityRenderStateAddition;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class MixinEntityRenderState implements EntityRenderStateAddition<Entity> {
    @Unique
    Entity entity;

    @Unique
    public Entity liquid_bounce$getEntity() {
        return entity;
    }

    @Unique
    public void liquid_bounce$setEntity(Entity entity) {
        this.entity = entity;
    }
}
