package net.ccbluex.liquidbounce.interfaces;

import net.minecraft.entity.Entity;

public interface EntityRenderStateAddition<T extends Entity> {
    T liquid_bounce$getEntity();
    void liquid_bounce$setEntity(T entity);
}
