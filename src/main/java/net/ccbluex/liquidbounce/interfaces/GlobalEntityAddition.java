package net.ccbluex.liquidbounce.interfaces;

import net.minecraft.util.math.Vec3d;

public interface GlobalEntityAddition {
    Vec3d liquidBounce$getActualPosition();
    boolean liquidBounce$getPassedFirstUpdate();
    void liquidBounce$updateFirstUpdate();
    void liquidBounce$setActualPosition(Vec3d newPosition);
}
