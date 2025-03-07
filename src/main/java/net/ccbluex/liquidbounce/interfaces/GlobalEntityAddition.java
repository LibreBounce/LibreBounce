package net.ccbluex.liquidbounce.interfaces;

import net.minecraft.util.math.Vec3d;

/**
 * Addition to {@link net.minecraft.entity.Entity}
 */
public interface GlobalEntityAddition {
    /**
     * This gets the entity's unfiltered position received by the server.
     * <p>
     * For context, Minecraft splits the received position update from the server into 3 ticks of interpolation,
     * so the player's movement looks nice and smooth to the user's screen. This ignores the interpolation
     * and gives the result of the actual position of the entity instead.
     */
    Vec3d liquidBounce$getActualPosition();

    /**
     * Updates the entity's position to the newest unfiltered position.
     */
    void liquidBounce$setActualPosition(Vec3d newPosition);

    /**
     * Checks if the entity already has their actual position updated once throughout their existence.
     * Useful in determining if the entity is living or not.
     */
    boolean liquidBounce$getPassedFirstUpdate();

    /**
     * Makes sure the entity has received their first position update.
     * This will let the client know that this entity is trusted enough to have their actual (server) position
     * visually displayed.
     */
    void liquidBounce$updateFirstUpdate();
}
